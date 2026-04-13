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
// Trong test, cần swap Dispatchers.IO -> TestCoroutineDispatcher
// để control timing và chạy synchronously.
//
// Nếu code hard-code Dispatchers.IO:
//   → không thể swap trong test → test flaky, async timing issues
// Với @Named inject:
//   -> test inject StandardTestDispatcher thay vào
// toàn bộ coroutine chạy synchronously,
//
// @Named("IO") / @Named("Main") / @Named("Default"):
//   → phân biệt 3 loại dispatcher, tránh nhầm lẫn khi inject
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
    // Dùng cho update UI, animation
    // Ít khi inject trực tiếp vì viewModelScope đã default main


    @Provides
    @Singleton
    @Named("Default")
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    // Dùng cho sorting, parsing JSON lớn, tính toán CPU heavy
    // Ví dụ: lọc/sort danh sách transaction lớn
}