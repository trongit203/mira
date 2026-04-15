package com.apollo.mira.domain.usecase

import com.apollo.mira.domain.model.DashboardSummary
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType
import com.apollo.mira.domain.repository.TransactionRepository
import com.apollo.mira.presentation.common.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// UseCase1: Lấy dashboard summary
class GetDashboardSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<UiState<DashboardSummary>> =
        repository.getRecentTransactions(limit = 20)
            .map { transactions ->

                if (transactions.isEmpty()) return@map UiState.Empty

                val income = transactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                
                val expense = transactions
                    .filter { it.type == TransactionType.EXPENSE }
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
                    retryable = true,
                    throwable = throwable
                ))
            }
    // Câu trả lời phỏng vấn: "Tại sao .catch() ở đây thay vì try/catch?"
    // .catch() chỉ bắt exception từ UPSTREAM (DB, mapping)
    // try/catch trong collector bắt cả exception từ collector (UI) -> không mong muốn
}

// UseCase 2: Thêm giao dịch
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

// UseCase 3: Lấy danh sách giao dịch (dùng cho màn hình khác)
class GetRecentTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<Transaction>> = 
        repository.getRecentTransactions(limit)
}