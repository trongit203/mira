package com.apollo.mira.utils

import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// ============================================================
// FAKE REPOSITORY — in-memory impl, dùng trong test
//
// Fake vs Mock — câu hỏi phỏng vấn:
// • Fake: implementation thật nhưng in-memory (không cần Room)
//   → dùng khi cần test Observable behavior (Flow emit nhiều lần)
// • Mock: mockk<TransactionRepository>()
//   → dùng khi chỉ cần verify "hàm X có được gọi không"
//
// Dùng Fake ở đây vì UseCase test cần Flow emit nhiều giá trị
// (simulate DB thay đổi) — Mock khó làm điều này tự nhiên
// ============================================================
class FakeTransactionRepository: TransactionRepository {

    // StateFlow thay cho DB - emit mỗi khi _transactions thay đổi
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())

    // Test có thể set trực tiếp để setup scenario
    fun setTransactions(transactions: List<Transaction>) {
        _transactions.value = transactions
    }

    var shouldThrowError = false
    var errorMessage = "Lỗi DB giả lập"

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = 
        _transactions.map { 
            if (shouldThrowError) throw RuntimeException(errorMessage)
            it.take(limit)
        }

    override fun getAllTransactions(): Flow<List<Transaction>> = _transactions

    override suspend fun addTransaction(transaction: Transaction): Result<Transaction> {
        if (shouldThrowError) return Result.failure(RuntimeException(errorMessage))
        val newId = (_transactions.value.maxOfOrNull { it.id } ?: 0L) + 1
        val saved = transaction.copy(id = newId)
        _transactions.update { current -> current + saved }
        return Result.success(saved)
    }

    override suspend fun deleteTransaction(id: Long): Result<Unit> {
        if (shouldThrowError) return Result.failure(RuntimeException(errorMessage))
        _transactions.update { current -> current.filter { it.id != id } }
        return Result.success(Unit)
    }

}