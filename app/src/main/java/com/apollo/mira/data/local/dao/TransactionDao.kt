package com.apollo.mira.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.apollo.mira.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("""
        SELECT * FROM transactions
        WHERE isDeleted = 0
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE isDeleted = 0
        ORDER BY timestamp DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // suspend vì là one-shot operation, không cần observer
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity): Long // return generated ID

    // Soft delete — không xoá thật, chỉ đánh dấu isDeleted
    @Query("UPDATE transactions SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

//    DUng cho testing  - xoa tat ca
    @Delete
    suspend fun deleteAll()
}