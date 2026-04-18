# ============================================================
# MIRA PROGUARD RULES
#
# R8 (successor của ProGuard) làm 3 việc:
# 1. Shrink: xoá code không dùng (giảm APK size)
# 2. Obfuscate: đổi tên class/method thành a, b, c...
# 3. Optimize: inline method, remove dead code
#
# Câu hỏi phỏng vấn: "ProGuard bảo vệ gì, không bảo vệ gì?"
# BẢO VỆ: business logic, algorithm, tên class/method
# KHÔNG bảo vệ: network traffic (cần SSL Pinning riêng),
#               strings hardcode trong code (API keys exposed!)
#               → KHÔNG bao giờ lưu API key trong source code
# ============================================================

# ── Giữ lại class cần thiết ───────────────────────────────────

# Domain models -
-keep class com.apollo.mira.domain.model.** { *; }
-keepclassmembers class com.apollo.mira.domain.model.** { *; }

# Data entities - Room cần tên field để map column
-keep class com.apollo.mira.data.local.entity.** { *; }

# Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Retrofit + Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class androidx.room3.** { *; }
-keep @androidx.room3.Entity class * { *; }
-keep @androidx.room3.Dao interface * { *; }

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Security-specific rules ───────────────────────────────────

# EncryptedSharedPreferences - AndroidX Security
-keep class androidx.security.crypto.** { *; }

# BiometricPrompt}
-keep class androidx.biometric.** { *; }

# ── Logging — xoá hoàn toàn trong release ────────────────────
# Xoá Timber log calls trong release build
# Không cần rule đặc biệt nếu dùng R8 full mode,
# nhưng đảm bảo không có Log.d() nào còn trong production:
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int w(...);
    public static int v(...);
}

# ── Stacktrace — giúp decode crash report ────────────────────
# Giữ line numbers để Firebase Crashlytics decode được stacktrace
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Crashlytics sẽ dùng mapping.txt để decode obfuscated stacktrace