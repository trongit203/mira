package com.apollo.mira.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// ============================================================
// MAIN DISPATCHER RULE - swap Dispatchers.Main trong test
// Vấn đề: ViewModel dùng viewModelScope chạy trên Dispatchers.Main
// Trong unit test không có Android Looper -> crash nếu không swap
//
// Giải pháp: JUnit Rule swap Main -> TestDispatcher trước mỗi test
// -> coroutine chạy synchronously, kiểm soát được timing
//
// Cách dùng:
// @get:Rule val mainDispatcherRule = MainDispatcherRule()
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
): TestWatcher() {
    override fun starting(description: Description) {
        // Trước mỗi test: swap Main -> TestDispatcher
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        // Sau mỗi test: restore Main thật
        Dispatchers.resetMain()
    }
}