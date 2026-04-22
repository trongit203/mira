package com.apollo.mira.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================
// BIOMETRIC MANAGER — quản lý xác thực vân tay / khuôn mặt
//
// Flow trong Mira:
// 1. User bật "Khoá app bằng vân tay" trong Settings
// 2. SecurePreferences.isBiometricEnabled = true
// 3. Mỗi lần mở app → MiraActivity gọi authenticate()
// 4. Thành công → tiếp tục | Thất bại → khóa app
//
// BiometricPrompt vs FingerprintManager:
// → FingerprintManager deprecated từ API 28
// → BiometricPrompt xử lý cả fingerprint, face, iris
//   và fallback về PIN/pattern nếu biometric không available
//
// Câu hỏi phỏng vấn: "Tại sao dùng BIOMETRIC_STRONG thay vì WEAK?"
// → STRONG: hardware-backed, không thể spoof bằng ảnh / video
// → WEAK: software-only, có thể bypass trên một số device
//   Với app tài chính → bắt buộc phải STRONG
// ============================================================
@Singleton
class MiraBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Channel để bridge callback-based API → Kotlin Flow
    private val _authResult = Channel<BiometricResult>()
    val authResult = _authResult.receiveAsFlow()

    // ── Availability check ────────────────────────────────────

    fun checkBiometricAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                BiometricAvailability.Available

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.NoHardware

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.HardwareUnavailable

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NoneEnrolled
                // User chưa setup vân tay → hướng dẫn vào Settings

            else -> BiometricAvailability.Unknown
        }
    }

    // ── Authentication ────────────────────────────────────────

    fun authenticate(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                // result.authenticationType: BIOMETRIC hoặc DEVICE_CREDENTIAL
                _authResult.trySend(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val result = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                        BiometricResult.UserCancelled

                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                        // Quá nhiều lần thử sai → lockout
                        BiometricResult.LockedOut

                    else -> BiometricResult.Error(errString.toString())
                }
                _authResult.trySend(result)
            }

            override fun onAuthenticationFailed() {
                // Gọi mỗi lần scan không khớp — KHÔNG phải lỗi cuối
                // Hệ thống tự xử lý retry, không cần làm gì ở đây
                // Chỉ log để analytics nếu cần
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực để tiếp tục")
            .setSubtitle("Dùng vân tay hoặc khuôn mặt để mở Mira")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            // DEVICE_CREDENTIAL: fallback về PIN/pattern nếu biometric fail 5 lần
            // Không set setNegativeButtonText() khi có DEVICE_CREDENTIAL
            .build()

        prompt.authenticate(promptInfo)
    }
}

// ── Result types ──────────────────────────────────────────────

sealed class BiometricResult {
    object Success : BiometricResult()
    object UserCancelled : BiometricResult()
    object LockedOut : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}

sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NoHardware : BiometricAvailability()
    object HardwareUnavailable : BiometricAvailability()
    object NoneEnrolled : BiometricAvailability()
    object Unknown : BiometricAvailability()
}
