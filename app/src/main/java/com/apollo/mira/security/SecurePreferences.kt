package com.apollo.mira.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Lazy init — chỉ tạo khi cần lần đầu
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        // MasterKey: AES256_GCM - tiêu chuẩn Android Keystore
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "mira_secure_refs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── API keys & tokens ────────────────────────────────────

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit {
            if (value != null) putString(KEY_AUTH_TOKEN, value)
            else remove(KEY_AUTH_TOKEN)
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit {
            if (value !== null) putString(KEY_REFRESH_TOKEN, value)
            else remove(KEY_REFRESH_TOKEN)
        }

    // ── User session ─────────────────────────────────────────

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_USER_ID, value) }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, value) }

    // ── Session management ───────────────────────────────────
    fun clearSession() {
        // Xoá tất cả khi logout - không xoá biometric preference
        prefs.edit {
            remove(KEY_AUTH_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
        }
    }

    fun clearAll() = prefs.edit { clear() }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enable"
    }
}