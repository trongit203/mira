package com.apollo.mira.security

import android.content.Context
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Singleton

@Singleton
class MiraBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _authResult = Channel<BiometricResult>()
    val authResult = _authResult.receiveAsFlow()

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
            .setTitle("Xác thực ngay để tiếp tục")
            .setSubtitle("Dùng vân tay hoặc khuôn mặt để mở Mira")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            // DEVICE_CREDENTIAL: fallback về PIN/pattern nếu biometric fail 5 lần
            // Không set setNegativeButtonText() khi có DEVICE_CREDENTIAL
            .build()

        prompt.authenticate(promptInfo)
    }
}

sealed class BiometricResult {
    object Success: BiometricResult()
    object UserCancelled: BiometricResult()
    object LockedOut: BiometricResult()
    data class Error(val message: String) : BiometricResult()
}

sealed class BiometricAvailability {
    object Available: BiometricAvailability()
    object NoHardware: BiometricAvailability()
    object HardwareUnavailable: BiometricAvailability()
    object NoneEnrolled: BiometricAvailability()
    object Unknown: BiometricAvailability()
}
