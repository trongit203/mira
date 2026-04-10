package com.apollo.mira.data.local

import android.annotation.SuppressLint
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.apollo.mira.data.local.dao.TransactionDao
import com.apollo.mira.data.local.entity.TransactionEntity
@SuppressLint("RestrictedApi")
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MiraDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "mira.db"
    }
}