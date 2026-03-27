package com.apollo.mira.presentation.dashboard

import com.apollo.mira.domain.model.Transaction

sealed class DashboardUiState {
    object Loading: DashboardUiState()

    data class Success (
        val totalBalance: Double,
        val totalIncome: Double,
        val totalExpense: Double,
        val recentTransactions: List<Transaction>
    ) : DashboardUiState()

    data class Error(
        val message: String,
        val retryAction: (() -> Unit)? = null
    ) : DashboardUiState()
}