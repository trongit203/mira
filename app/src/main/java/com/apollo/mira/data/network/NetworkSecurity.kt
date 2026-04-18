package com.apollo.mira.data.network

import com.apollo.mira.security.SecurePreferences
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ============================================================
// NETWORK SECURITY — SSL Pinning + Security Interceptors
//
// SSL Pinning hoạt động thế nào:
// 1. Browser/app bình thường: trust bất kỳ cert nào được CA ký
//    → Attacker dùng Charles Proxy tạo cert giả được CA ký → MITM thành công
//
// 2. Với SSL Pinning: app chỉ trust cert CỤ THỂ đã được hardcode
//    → Dù Attacker có cert hợp lệ của CA khác → bị reject
//    → Charles Proxy không còn tác dụng
//
// Lấy SHA-256 hash của cert:
//   $ openssl s_client -connect api.mira.vn:443 | openssl x509 -pubkey -noout | \
//     openssl rsa -pubin -outform der | openssl dgst -sha256 -binary | base64
//
// Hoặc dùng OkHttp logging để lấy hash (xem comment trong MiraApp)
//
// Câu hỏi phỏng vấn: "SSL Pinning giải quyết vấn đề gì?"
// → Giải quyết MITM attack kể cả khi attacker có certificate
//   được ký bởi Certificate Authority hợp lệ (corporate proxy,
//   rogue CA, hoặc compromised CA)
// ============================================================

object NetworkSecurity {

    // ── Certificate Pinning ───────────────────────────────────

    private val certificatePinner = CertificatePinner.Builder()
        // Format: "sha256/<base64-hash>"
        // Cần 2 hash: cert hiện tại + backup (phòng khi cert expire)
        // THAY BẰNG HASH THẬT của server khi production
        .add("api.mira.vn", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")  // primary
        .add("api.mira.vn", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")  // backup
        // Thêm subdomain nếu có:
        // .add("*.mira.vn", "sha256/...")
        .build()

    // ── Security Interceptor ──────────────────────────────────

    class SecurityInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                // Thêm security headers vào mọi request
                .addHeader("X-App-Version", com.apollo.mira.BuildConfig.VERSION_NAME)
                .addHeader("X-Platform", "android")
                // Content-Type đảm bảo không bị content-type sniffing
                .addHeader("Accept", "application/json")
                .build()

            return chain.proceed(request)
        }
    }

    // ── OkHttpClient builder ──────────────────────────────────

    fun buildSecureOkHttpClient(
        isDebug: Boolean,
        authInterceptor: AuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder().apply {

        // Timeout — tránh connection hang mãi mãi
        connectTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)

        // SSL Pinning — chỉ bật PRODUCTION
        // Debug: tắt pinning để dùng Charles proxy khi dev
        if (!isDebug) {
            certificatePinner(certificatePinner)
        }

        // Auth token injection
        addInterceptor(authInterceptor)

        // Security headers
        addInterceptor(SecurityInterceptor())

        // Logging — chỉ debug
        if (isDebug) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    // Trong log, redact sensitive headers
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                }
            )
            // Tip: lần đầu setup pinning, dùng Level.BODY để thấy
            // "Certificate pinning failure!" message với hash đúng cần dùng
        }

    }.build()
}

// ── Auth Interceptor ──────────────────────────────────────────

class AuthInterceptor @Inject constructor(
    private val securePreferences: SecurePreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Không thêm token cho endpoint auth (login, refresh)
        if (originalRequest.url.encodedPath.contains("/auth/")) {
            return chain.proceed(originalRequest)
        }

        val token = securePreferences.authToken
            ?: return chain.proceed(originalRequest) // unauthenticated request

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // 401 → token expired → trigger refresh flow
        if (response.code == 401) {
            response.close()
            // Trong production: gọi refresh token endpoint
            // Nếu refresh fail → clear session, navigate to login
            securePreferences.clearSession()
        }

        return response
    }
}

