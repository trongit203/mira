package com.apollo.mira.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class TransactionType { INCOME, EXPENSE }