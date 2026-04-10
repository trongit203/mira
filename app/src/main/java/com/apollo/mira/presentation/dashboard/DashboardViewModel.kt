package com.apollo.mira.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.usecase.GetDashboardSummaryUseCase
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import com.apollo.mira.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val addTransaction: AddTransactionUseCase
) : ViewModel() {

    // ======= STATE FLOW - Cho UI state (màn hình)
    val uiState = getDashboardSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    // ======= SHARED FLOW - Cho one time event
    // replay = 0 ko replay event cũ cho subscribe mới
    // -> tránh toast """Thêm thành công" hiện lại khi user quay lại màn hình
    private val _events = MutableSharedFlow<DashboardEvent>(replay = 0)
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
            _events.emit(DashboardEvent.NavigateToDetail(transactionId))
        }
    }

    fun onQuickAddTransaction(transaction: Transaction) {
        viewModelScope.launch {
            addTransaction(transaction)
                .onSuccess { 
                    _events.emit(DashboardEvent.ShowSnackbar("Đã thêm giao dịch ✓"))
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

