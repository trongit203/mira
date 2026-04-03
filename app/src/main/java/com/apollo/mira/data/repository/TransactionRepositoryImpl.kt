package com.apollo.mira.data.repository

import com.apollo.mira.data.local.dao.TransactionDao
import com.apollo.mira.data.mapper.TransactionMapper
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class TransactionRepositoryImpl @Inject constructor (
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    @Named("IO") private val dispatcher: CoroutineDispatcher
): TransactionRepository {
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        dao.getRecentTransactions(limit)
            .map { entities -> mapper.toDomainList(entities) }
            .flowOn(dispatcher)

    override fun getAllTransactions(): Flow<List<Transaction>> =
            dao.getAllTransactions()
                .map { entities -> mapper.toDomainList(entities) }
                .flowOn(dispatcher)

    override suspend fun addTransaction(transaction: Transaction) : Result<Transaction> =
        withContext(dispatcher) {
            runCatching {
                val entity = mapper.toEntity(transaction)
                val generateId = dao.insert(entity)

                // trả về domain object với ID thật từ DB
                transaction.copy(id = generateId)
            }
    }

    override suspend fun deleteTransaction(id: Long): Result<Unit> {
        TODO("Not yet implemented")
    }
}