package com.apollo.mira.domain.usecase

import app.cash.turbine.test
import com.apollo.mira.presentation.common.UiState
import com.apollo.mira.utils.FakeTransactionRepository
import com.apollo.mira.utils.MainDispatcherRule
import com.apollo.mira.utils.TransactionFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// ============================================================
// GET DASHBOARD SUMMARY USE CASE TEST
//
// Dùng FakeRepository (không phải Mock) vì cần test
// Flow emit nhiều lần — simulate DB thay đổi realtime
//
// Turbine: thư viện test Flow của CashApp
// .test { } — collect Flow trong test scope
// awaitItem() — chờ emit tiếp theo
// cancelAndIgnoreRemainingEvents() — kết thúc test
// ============================================================
@OptIn(ExperimentalCoroutinesApi::class)
class GetDashboardSummaryUseCaseTest {

    // Rule này swap Dispatcher.Main -> TestDispatcher
    // BẮT BUỘC cho mọi test liên quan ViewModel hoặc viewModelScope
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Dùng Fake, không phải Mock — lý do giải thích trong FakeTransactionRepository
    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var useCase: GetDashboardSummaryUseCase

    @Before
    fun setup() {
        fakeRepository = FakeTransactionRepository()
        useCase = GetDashboardSummaryUseCase(fakeRepository)
    }

    // ── Happy path tests ─────────────────────────────────────
    @Test
    fun `when a transaction occurs, emit Success with the correct total amount`() = runTest {
        val transactions = TransactionFixtures.aListOfTransactions()
        fakeRepository.setTransactions(transactions)

        // Act + Assert - Turbine collect flow
        useCase().test {
            val state = awaitItem()

            assertTrue("Expected Success but got $state", state is UiState.Success)

            // Smart cast sau khi check - không cần cast thủ công
            val summary = (state as UiState.Success).data

            // Assert business logic: income và expense tính đúng không?
            assertEquals(10_000_000.0, summary.totalIncome, 0.001)
            assertEquals(400_000.0, summary.totalExpense, 0.001)
            assertEquals(9_600_000.0, summary.netBalance, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when no transactions, emit Empty state`() = runTest {
        // Arrange - không set transactions -> default empty list
        // fakeRepository đã init với emptyList
        useCase().test {
            val state = awaitItem()
            assertTrue("Expect Empty but got $state", state is UiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savings rate being calculate correctly`() = runTest {
//        income = 10_000_000, expense = 400_000
//        savingsRate = 10_000_000 - 400_000 / 10_000_000 = 0.96

        fakeRepository.setTransactions(TransactionFixtures.aListOfTransactions())

        useCase().test {
            val summary = (awaitItem() as UiState.Success).data
            assertEquals(0.96, summary.savingsRate, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when only have income, expensive = 0 and balance = income`() = runTest {
        fakeRepository.setTransactions(listOf(TransactionFixtures.anIncome(5_000_000.0)))

        useCase().test {
            val summary = (awaitItem() as UiState.Success).data
            assertEquals(5_000_000.0, summary.totalIncome, 0.001)
            assertEquals(0.0, summary.totalExpense, 0.001)
            assertEquals(5_000_000.0, summary.netBalance, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Error path tests ─────────────────────────────────────
    @Test
    fun `when repository throw exception, emit Error state`() = runTest {
        // Arrange — simulate DB crash
        fakeRepository.shouldThrowError = true
        fakeRepository.errorMessage = "Lỗi kết nối database"

        useCase().test {
            val state = awaitItem()
            assertTrue("Expect Error but got $state", state is UiState.Error)

            val error = state as UiState.Error
            println("error.message = ${error.message}")
            println("error.retryable = ${error.retryable}")
            assertEquals("Lỗi kết nối database", error.message)
            assertTrue("Error should be retryable", error.retryable )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Reactive behavior tests ──────────────────────────────
    //  Test quan trọng nhất - verify flow emit lại khi DB thay đổi
    // Mock không làm được điều này tự nhiên - lý do dùng Fake

    @Test
    fun `when add new transaction, Flow emit new data automatically`()  = runTest {
        useCase().test {
            // Emit 1: empty list ban đầu
            assertTrue(awaitItem() is UiState.Empty)
            // Simulate user thêm giao dịch mới
            fakeRepository.setTransactions(listOf(TransactionFixtures.anExpense()))

            // Emit 2: Flow tự update không cần gọi lại
            val updated = awaitItem()
            assertTrue("Expect Success after adding transaction", updated is UiState.Success)
            assertEquals(150_000.0, (updated as UiState.Success).data.totalExpense, 0.001)
        }
    }

    @Test
    fun `recentTransactions are correctly limited`() = runTest {
        // Tạo 25 transactions — repo.getRecentTransactions(limit = 20)
        val manyTransactions = (1..25).map { i ->
            TransactionFixtures.anExpense(amount = i * 10_000.0)
        }
        fakeRepository.setTransactions(manyTransactions)

        useCase().test {
            val summary = (awaitItem() as UiState.Success).data
            assertEquals(20, summary.recentTransactions.size)
            cancelAndIgnoreRemainingEvents()
       }
    }
}