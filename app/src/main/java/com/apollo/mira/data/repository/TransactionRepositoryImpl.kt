package com.apollo.mira.data.repository

import com.apollo.mira.data.local.dao.TransactionDao
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl @Inject constructor (
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    private val dispatcher: CoroutineDispatcher
): TransactionRepository {

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = dao.getRecentTransactions(limit)
        .map { entities -> entities.map(mapper::toDomain) }
        .flowOn(dispatcher)

    override suspend func addTransaction(transaction: Transaction) : Result<Long> =
        withContext(dispatcher) {
            runCatching {
                val entity = mapper.toEntity(transaction)
                dao.insertTransaction(entity)
            }
    }
}