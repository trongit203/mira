package com.apollo.mira.di

import com.apollo.mira.BuildConfig
import com.apollo.mira.data.network.AuthInterceptor
import com.apollo.mira.data.network.NetworkSecurity
import com.apollo.mira.security.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// ============================================================
// NETWORK MODULE — wire OkHttp + Retrofit với security

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(securePreferences: SecurePreferences): AuthInterceptor = AuthInterceptor(securePreferences)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient = NetworkSecurity.buildSecureOkHttpClient(
        isDebug = BuildConfig.DEBUG,
        authInterceptor = authInterceptor
    )

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.mira.vn/v1")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}