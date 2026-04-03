package com.apollo.mira.domain.repository

import com.apollo.mira.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
// ============================================================
// REPOSITORY INTERFACE - domain layer định nghĩa CONTRACT
// Domain ko biết Room hay Retrofit tồn tại
// Data layer implement interface này
// -> Đây là Dependency Inversion (chữ D trong SOLID)
interface TransactionRepository {
    // Trả về Flow - Room tự emit mỗi khi DB thay đổi
    // Cold flow: chỉ chạy khi có collector
    fun getRecentTransactions(limit: Int = 20): Flow<List<Transaction>>

    fun getAllTransactions(): Flow<List<Transaction>>

    // suspend + Result<T>: không throw exception, caller tự xử lý
    suspend fun addTransaction(transaction: Transaction): Result<Transaction>

    suspend fun deleteTransaction(id: Long): Result<Unit>
}