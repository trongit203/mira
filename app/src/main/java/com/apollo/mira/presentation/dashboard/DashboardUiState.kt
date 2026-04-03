package com.apollo.mira.presentation.dashboard

sealed class DashboardEvent {
    data class ShowSnackbar(val message: String): DashboardEvent()
    data class NavigateToDetail(val transactionId: Long): DashboardEvent()
    object NavigateToAddTransaction: DashboardEvent()
}