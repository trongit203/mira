package com.apollo.mira.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.usecase.GetDashboardSummaryUsecase
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed

// presentation/dashboard/DashboardViewModel.kt
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUsecase,
    private val addTransaction: AddTransactionUsecase
) : ViewModel() {

    // ======= STATE FLOW - Cho UI state (màn hịnh)
    val uiState: StateFlow<DashboardUiState> =
            getDashboardSummary()
                .stateIn(
                    scope        = viewModelScope,
                    started      = SharingStarted.WhileSubscribed(5_000),
                    // WhileSubscribed(5000): Giữ Flow active 5 giây sau khi UI off screen
                    // -> tránh restart khi xoay màn hình (<5 giây)
                    // -> cancel khi user thực sự rời màn hĩnh
                    initialValue = DashboardUiState.Loading
                )

    // ======= SHARED FLOW - Cho one time event
    // replay = 0 ko replay event cũ cho subscribe mới
    // -> tránh toast """Thêm thành công" hiện lại khi user quay lại màn hình
    private val _event = MutableSharedFlow<DashboardEvent>(replay = 0)
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    // ── ACTIONS ───────────────────────────────────────────────
    // User actions từ UI -> ViewModel xử lý -> emit state hoặc event
    fun onAddTransactionClick() { 
        viewModelScope.launch {
            _events.emit(DashboardEvent.NavigateToAddTransaction)
        }
    }

    fun onTransactionClick(transactionId: Long) { 
        viewModelScope.launch {
            _events.emit(DashboardEvent.NavigateToAddTransaction(transactionId))
        }
    }

    fun onQuickAddTransaction(transaction: Transaction) {
        viewModelScope.launch {
            addTransaction(transaction)
                .onSuccess { 

                }
                .onFailure { error ->
                    _events.emit(
                        DashboardEvent.ShowSnackbar(
                            error.message ?: "Lỗi khi thêm giao dịch"
                        )
                    )
                }
        }
    }
}

sealed class DashboardEvent {
    data class ShowSuccess(val message: String) : DashboardEvent()
    data class ShowError(val message: String) : DashboardEvent()
    data class NavigateTo(val route: String)
}
