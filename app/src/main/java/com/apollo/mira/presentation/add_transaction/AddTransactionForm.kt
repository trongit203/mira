package com.apollo.mira.presentation.add_transaction

import com.apollo.mira.domain.model.TransactionType

data class AddTransactionForm(
    val rawAmount: String = "",
    val note: String = "",
    val selectedCategory: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val dateMillies: Long? = null,

    // Validation state - tinh tu raw values
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSubmitting: Boolean = false
) {
    val parsedAmount: Double get() = rawAmount.toDoubleOrNull() ?: 0.0
    val isValid: Boolean get() =
        parsedAmount > 0 &&
        selectedCategory.isNotBlank() &&
        amountError == null &&
        categoryError == null
}

sealed class AddTransactionEvent {
    object NavigateBack : AddTransactionEvent()
    data class ShowError(val message: String): AddTransactionEvent()
}