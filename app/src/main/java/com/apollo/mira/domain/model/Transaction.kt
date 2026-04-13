package com.apollo.mira.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),
    val source: DataSource = DataSource.MANUAL,
)

enum class DataSource { MANUAL, OCR, MOMO, ZALOPAY, BANK_SYNC }

enum class TransactionType { INCOME, EXPENSE }

data class DashboardSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList()
) {
    val netBalance: Double get() = totalIncome - totalExpense
    val savingsRate: Double get() = if (totalIncome > 0) netBalance / totalIncome else 0.0
    }