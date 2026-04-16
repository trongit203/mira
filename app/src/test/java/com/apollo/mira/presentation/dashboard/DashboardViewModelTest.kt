package com.apollo.mira.presentation.dashboard

import app.cash.turbine.test
import com.apollo.mira.domain.model.DashboardSummary
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import com.apollo.mira.domain.usecase.GetDashboardSummaryUseCase
import com.apollo.mira.presentation.common.UiState
import com.apollo.mira.utils.MainDispatcherRule
import com.apollo.mira.utils.TransactionFixtures
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Dùng mock (không phải fake) cho UseCase bởi vì:
// • ViewModel test không cần verify reactive Flow chain
// • Chỉ cần biết "khi UseCase trả X, ViewModel xử lý thế nào"
// • mockk() đơn giản hơn, tường minh hơn cho tầng này
//
// MockK syntax quan trọng:
//  • every { }     - stub cho non-suspend function
//  • coEvery { }   - stub cho suspend function (co = coroutine)
//  • verify { }    - verify non-suspend call
//  • coVerify { }  - verify suspend call
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    // mockk<T>(): tạo mock hoàn toàn - tất cả function return default
    // relaxed = true: function chưa stub trả về default thay vì throw
    private val getDashboardSummary = mockk<GetDashboardSummaryUseCase>()
    private val addTransaction = mockk<AddTransactionUseCase>(relaxed = true)

    private lateinit var viewModel: DashboardViewModel

    // Setup KHÔNG tạo ViewModel ở đây — từng test setup riêng
    // Lý do: mỗi test cần stub khác nhau trước khi ViewModel init
    private fun createViewModel(): DashboardViewModel = DashboardViewModel(getDashboardSummary, addTransaction)

    // ── uiState tests ─────────────────────────────────────────
    @Test
    fun `initial state is loading`() = runTest {
        // Stub UseCase trả Flow không emit ngay (Flow treo)
        every { getDashboardSummary() } returns flowOf<UiState<DashboardSummary>>(UiState.Loading)

        val vm = createViewModel()

        assertEquals(UiState.Loading, vm.uiState.value)
    }

    @Test
    fun `when UseCase emit Success, uiState transform to Success`() = runTest {
        val summary = DashboardSummary(
            totalIncome = 10_000_000.0,
            totalExpense = 400_000.0,
            recentTransactions = TransactionFixtures.aListOfTransactions()
        )
        // Stub: getDashboardSummary() trả về Flow emit Success
        every { getDashboardSummary() } returns flowOf(UiState.Success(summary))

        val vm = createViewModel()
        vm.uiState.test {
            // Skip Loading (initialValue của stateIn)
            val initial = awaitItem()
            assertTrue(initial is UiState.Loading)

            advanceUntilIdle()
            val success = awaitItem()
            assertTrue("Expected Success but got $success", success is UiState.Success)

            val data = (success as UiState.Success).data
            assertEquals(10_000_000.0, data.totalIncome, 0.001)
            assertEquals(400_000.0, data.totalExpense, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when UseCase emit Error, uiState transform to Error`() = runTest {
        every { getDashboardSummary() } returns flowOf(
            UiState.Error("Lỗi kết nối", retryable = true)
        )

        val vm = createViewModel()

        vm.uiState.test {
            // 1. Nhặt Loading
            assertTrue(awaitItem() is UiState.Loading)

            //2. Đợi và nhặt Error
            val state = awaitItem()
            assertTrue("Expected Error but got $state" , state is UiState.Error)
            assertEquals("Lỗi kết nối", (state as UiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when UseCase emit Empty, uiState transform to Empty`() = runTest {
        every { getDashboardSummary() } returns flowOf(UiState.Empty)

        val vm = createViewModel()

        vm.uiState.test {

            val firstItem = awaitItem()

            advanceUntilIdle()

            val secondItem = awaitItem()

            assertTrue("Expected Empty but got $secondItem", secondItem is UiState.Empty)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Events tests ──────────────────────────────────────────

    @Test
    fun `onAddTransactionClick emit NavigateToTransactionScreen event`() = runTest {
        every { getDashboardSummary() } returns flowOf(UiState.Empty)

        val vm = createViewModel()

        vm.events.test {
            vm.onAddTransactionClick()
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue("Expected NavigateToAddTransaction but got $event", event is DashboardEvent.NavigateToAddTransaction)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTransactionClick emit NavigateToDetail with correct ID`() = runTest {
        every { getDashboardSummary() } returns flowOf(UiState.Empty)
        val vm = createViewModel()
        vm.events.test {
            vm.onTransactionClick(transactionId = 42L)
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is DashboardEvent.NavigateToDetail)
        }
    }

    @Test
    fun `when addTransaction success, emit ShowSnackbar with success message`() = runTest {
        every { getDashboardSummary() } returns flowOf(UiState.Empty)
        coEvery { addTransaction(any()) } returns Result.failure(Exception("Lỗi lưu DB"))

        val vm = createViewModel()
        vm.events.test {
            vm.onQuickAddTransaction(TransactionFixtures.anExpense())
            advanceUntilIdle()
            val event = awaitItem() as DashboardEvent.ShowSnackbar
            assertEquals("Lỗi lưu DB", event.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when addTransaction fail, emit ShowSnackbar with error message`() = runTest {
        every { getDashboardSummary() } returns flowOf(UiState.Empty)

        coEvery { addTransaction(any()) } returns Result.failure(Exception("Lỗi lưu DB"))

        val vm = createViewModel()
        vm.events.test {
            vm.onQuickAddTransaction(TransactionFixtures.anExpense())
            advanceUntilIdle()

            val event = awaitItem() as DashboardEvent.ShowSnackbar
            assertEquals("Lỗi lưu DB", event.message)

            cancelAndIgnoreRemainingEvents()
        }

    }

    @Test
    fun `addTransaction being called with correct transaction object`() = runTest {
        every { getDashboardSummary() } returns flowOf(UiState.Empty)
        coEvery { addTransaction(any()) } returns Result.success(TransactionFixtures.anExpense())

        val vm = createViewModel()
        val transaction = TransactionFixtures.anExpense(amount = 999_000.0)

        vm.onQuickAddTransaction(transaction)

        advanceUntilIdle()

        // coVerify: đảm bảo addTransaction được gọi đúng với object
        coVerify(exactly = 1) { addTransaction(transaction) }
    }
}