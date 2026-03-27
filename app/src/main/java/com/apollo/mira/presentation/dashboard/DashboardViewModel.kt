package com.apollo.mira.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.usecase.GetDashboardSummaryUsecase
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed

class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUsecase,
    private val addTransaction: AddTransactionUsecase
) : ViewModel() {
    // ======= STATE FLOW - Cho UI state (màn hịnh)
    val uiState: StateFlow<DashboardUiState> =
            getDashboardSummary()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DashboardUiState.Loading
                )

    // ======= SHARED FLOW - Cho one time event
    // replay = 0 ko replay event cũ cho subscribe mới
    // -> tránh toast """Thêm thành công" hiện lại khi user quay lại màn hình
    private val _event = MutableSharedFlow<DashboardEvent>(replay = 0)
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    fun onAddTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val result = addTransaction(transaction)
                result.fold(
                    onSuccess =  { _events.emit(DashboardEvent.ShowSuccess("Đã thêm giao dịch")) },
                    onFailure =  { _events.emit(DashboardEvent.ShowError(it.message ?: "Lỗi")) }
                )
        }
    }
}

sealed class DashboardEvent {
    data class ShowSuccess(val message: String) : DashboardEvent()
    data class ShowError(val message: String) : DashboardEvent()
    data class NavigateTo(val route: String)
}
