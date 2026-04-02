package com.apollo.mira.domain.usecase

import com.apollo.mira.presentation.dashboard.DashboardUiState

// UseCase1: Lấy dashboard summary
class GetDashboardSummaryUsecase @Inject constructor(
    private val repository: TransactionRepository
) {

    operator fun invoke(): Flow<DashboardUiState> =
        repository.getRecentTransactionList(limit = 10)
            .map { transactions ->
                val income = transactions
                    .filter { it.type == INCOME }
                    .sumOf { it.amount }
                
                val expense = transactions
                    .filter { it.type == EXPENSE }
                    .sumOf { it.amount }

                val summary = DashboardSummary(
                    totalIncome = income,
                    totalExpense = expense,
                    recentTransactions = transactions
                )

                UiState.Success(summary) as UiState<DashboardSummary>
            }
            .catch { throwable ->
                emit(UiState.Error(
                    message = throwable.message ?: "Lỗi tải dữ liệu",
                    retryable = true
                ))
            }
    // Câu trả lời phỏng vấn: "Tại sao .catch() ở đây thay vì try/catch?"
    // .catch() chỉ bắt exception từ UPSTREAM (DB, mapping)
    // try/catch trong collector bắt cả exception từ collector (UI) -> không mong muốn
}

// Usecase 2: Thêm giao dịch
class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Transaction> { 
        // Validate trước khi lưu
        val validated = transaction.also { 
            require(it.amount > 0) { "Số tiền phải lớn hơn 0" }
            require(it.category.isNotBlank()) { "Phải chọn danh mục" }
        }
        return repository.addTransaction(validated)
    }
}

// Usecase 3: Lấy danh sách giao dịch (dùng cho màn hình khác)
class GetRecentTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<Transaction>> = 
        repository.getRecentTransactions(limit)
}