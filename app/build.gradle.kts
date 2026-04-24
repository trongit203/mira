plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler) // Apply plugin tại đây
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.apollo.mira"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.apollo.mira"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {

        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.room3.common.jvm)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.ui.test.junit4)
    implementation(libs.androidx.runtime)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)

    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    // Network
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)

    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.hilt.android.testing)
    testImplementation(kotlin("test"))


    ksp(libs.hilt.compiler)
    ksp(libs.androidx.room.compiler)
}