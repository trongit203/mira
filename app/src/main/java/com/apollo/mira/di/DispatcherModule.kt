package com.apollo.mira.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

// ============================================================
// DISPATCHER MODULE
// Tại sao inject Dispatcher thay vì dùng Dispatchers.IO trực tiếp?
//
// ============================================================

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @Singleton
    @Named("IO")
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
    // Dùng cho: network call, database query, file I/O
    // Đã dùng trong: TransactionRepositoryImpl

    @Provides
    @Singleton
    @Named("Main")
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    // Dùng cho update UI, animatin
    // Ít khi inject trực tiếp vì viewModelScope đã default main


    @Provides
    @Singleton
    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    // Dùng cho sorting, parsing JSON lớn, tính toán CPU heavy
    // Ví dụ: lọc/  danh sánh transaction lớn
}