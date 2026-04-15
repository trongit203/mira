package com.apollo.mira.presentation.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransaction: AddTransactionUseCase
) : ViewModel() {

    // Form state - MutableStateFlow vì update từ user input
    private val _form = MutableStateFlow(AddTransactionForm())
    val form: StateFlow<AddTransactionForm> = _form.asStateFlow()

    // Events - SharedFlow replay=0 vì one-time
    private val _events = MutableSharedFlow<AddTransactionEvent>(replay = 0)
    val events: SharedFlow<AddTransactionEvent> = _events.asSharedFlow()

    // ── User actions ─────────────────────────────────────────
    fun onAmountChange(raw: String) {
        _form.update { current ->
            current.copy(
                rawAmount = raw,
                // Validate ngay khi gõ - UX tốt hơn validate khi submit
                amountError = when {
                    raw.isBlank()                       -> null
                    raw.toDoubleOrNull() == null        -> "Số tiền không hợp lệ"
                    raw.toDouble() <= 0                 -> "Số tiền phải lớn hơn 0"
                    raw.toDouble() > 1_000_000_000      -> "Số tiền vượt quá giới hạn"
                    else                                -> null
                }
            )
        }
    }

    fun onNoteChange(note: String) {
        _form.update { it.copy(note = note) }
    }

    fun onCategorySelected(category: String) {
        _form.update { current ->
            current.copy(
                selectedCategory = category,
                categoryError = null
            )
        }
    }

    fun onTypeToggle(type: TransactionType) {
        _form.update { it.copy(transactionType = type) }
    }

    fun onSubmit() {
        val currentForm = _form.value

        // Validate toàn bộ form trước khi submit
        if (!currentForm.isValid) {
            _form.update { it.copy(
                amountError     = it.amountError,
                categoryError   = if (it.selectedCategory.isBlank()) "Chọn danh mục" else null
            )}
            return
        }

        viewModelScope.launch {
            _form.update { it.copy(isSubmitting = true) }

            val transaction = Transaction(
                amount          = currentForm.parsedAmount,
                category        = currentForm.selectedCategory,
                note            = currentForm.note.trim(),
                type            = currentForm.transactionType,
                timestamp       = currentForm.dateMillies
                                ?: System.currentTimeMillis()
            )

            addTransaction(transaction)
                .onSuccess {
                    _events.emit(AddTransactionEvent.NavigateBack)
                }
                .onFailure { error ->
                    _form.update { it.copy(isSubmitting = false) }
                    _events.emit(
                        AddTransactionEvent.ShowError(
                            error.message ?: "Không thể thêm giao dịch"
                        )
                    )
                }
        }
    }
}