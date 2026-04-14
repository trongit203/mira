package com.apollo.mira.utils

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType

object TransactionFixtures {
    fun aTransaction(
        id: Long = 1L,
        amount: Double = 150_000.0,
        category: String = "Ăn uống",
        note      : String           = "Phở bò Thìn",
        type      : TransactionType = TransactionType.EXPENSE,
        timestamp : Long             = 1_700_000_000_000L,
        source    : DataSource = DataSource.MANUAL
    ) = Transaction(
        id = id,
        amount = amount,
        category = category,
        note = note,
        type = type,
        timestamp = timestamp,
        source = source
    )

    fun anIncome(amount: Double = 5_000_000.0) = aTransaction(
        amount = amount,
        category = "Lương",
        note = "Lương tháng 12",
        type = TransactionType.INCOME
    )

    fun anExpense(amount: Double = 150_000.0) = aTransaction(
        amount   = amount,
        category = "Ăn uống",
        type     = TransactionType.EXPENSE
    )

    fun aListOfTransactions() = listOf(
        anIncome(amount = 10_000_000.0),
        anExpense(amount = 150_000.0),
        anExpense(amount = 50_000.0),
        anExpense(amount = 200_000.0)
    )
    // netBalance = 10_000_000 - 400_000 = 9_600_000
    // savingsRate = 9_600_000 / 10_000_000 = 0.96
}
