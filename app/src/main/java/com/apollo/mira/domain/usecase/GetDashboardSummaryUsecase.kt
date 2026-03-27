package com.apollo.mira.domain.usecase

import com.apollo.mira.presentation.dashboard.DashboardUiState

class GetDashboardSummaryUsecase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(): Flow<DashboardUiState> =
        repository.getRecentTransactionList(limit = 10)
            .map { transactions ->
                val income = transactions.filter { it.type == INCOME }.sumOf { it.amount }
                val expense = transactions.filter { it.type == EXPENSE }.sumOf { it.amount }

                DashboardUiState.Success(
                    totalBalance = income - expense,
                    totalIncome = income,
                    totalExpense = expense,
                    recentTransactions = transactions
                )
            }
            .catch { throwable ->
                emit(DashboardUiState.Error(
                    message = throwable.message ?: "Unknown error",
                    retryAction = null
                ))
            }
    // Câu trả lời phỏng vấn: "Tại sao .catch() ở đây thay vì try/catch?"
    // .catch() chỉ bắt exception từ UPSTREAM (DB, mapping)
    // try/catch trong collector bắt cả exception từ collector (UI) -> không mong muốn
}