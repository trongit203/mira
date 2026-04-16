package com.apollo.mira.presentation.dashboard

import com.apollo.mira.domain.model.DashboardSummary
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import com.apollo.mira.domain.usecase.GetDashboardSummaryUseCase
import com.apollo.mira.presentation.common.UiState
import com.apollo.mira.utils.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

// Dùng mock (không phải fake) cho UseCase bởi vì:
// • ViewModel test không cần verify reactive Flow chain
// • Chỉ cần biết "khi UseCase trả X, ViewModel xử lý thế nào"
// • mockk() đơn giản hơn, tường minh hơn cho tầng này

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

    }

    @Test
    fun `when UseCase emit Error, uiState transform to Error`() = runTest {

    }

    @Test
    fun `when UseCase emit Empty, uiState transform to Empty`() = runTest {

    }

    @Test
    fun `onAddTransactionClick emit NavigateToTransactionScreen event`() = runTest {

    }

    @Test
    fun `onTransactionClick emit NavigateToDetail with correct ID`() = runTest {

    }

    @Test
    fun `when addTransaction success, emit ShowSnackbar with success message`() = runTest {

    }

    @Test
    fun `when addTransaction fail, emit ShowSnackbar with error message`() = runTest {

    }

    @Test
    fun `addTransaction being called with correct transactioon object`() = runTest {

    }

}