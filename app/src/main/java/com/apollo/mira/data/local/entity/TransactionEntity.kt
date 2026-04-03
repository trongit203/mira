package com.apollo.mira.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// ============================================================
// TRANSACTION ENTITY — Room database model
//
// KHÁC với domain Transaction:
// • Dùng var (mutable) để Room có thể set giá trị
// • type và source lưu dạng String (enum không lưu được trực tiếp)
// • Có isDeleted cho soft delete — domain model không cần biết
// ============================================================
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var amount: Double = 0.0,
    var category: String = "",
    var note: String = "",
    var type: String = "EXPENSE", // TransactionType.name
    var timestamp: Long = 0L,
    var source: String = "MANUAL",
    var isDeleted: Boolean = false,
    var syncedAt: Long? = null
)