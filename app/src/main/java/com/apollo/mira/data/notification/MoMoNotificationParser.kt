package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType

class MoMoNotificationParser : NotificationParser {

    override fun canHandle(packageName: String): Boolean =
        packageName == "com.mservice.momotransfer"

    override fun parse(packageName: String, title: String, text: String, timestamp: Long): Transaction? {
        val amount = extractAmount(text) ?: return null
        return when {
            RECEIVE_RE.containsMatchIn(text) -> Transaction(
                amount    = amount,
                category  = CategoryInferenceEngine.inferIncomeCategory(text),
                note      = text.trim(),
                type      = TransactionType.INCOME,
                timestamp = timestamp,
                source    = DataSource.MOMO
            )
            SEND_RE.containsMatchIn(text) -> Transaction(
                amount    = amount,
                category  = "Chuyển khoản",
                note      = text.trim(),
                type      = TransactionType.EXPENSE,
                timestamp = timestamp,
                source    = DataSource.MOMO
            )
            PAYMENT_RE.containsMatchIn(text) -> {
                val merchant = PAYMENT_RE.find(text)?.groupValues?.getOrNull(1).orEmpty()
                Transaction(
                    amount    = amount,
                    category  = CategoryInferenceEngine.inferExpenseCategory("$text $merchant"),
                    note      = text.trim(),
                    type      = TransactionType.EXPENSE,
                    timestamp = timestamp,
                    source    = DataSource.MOMO
                )
            }
            else -> null
        }
    }

    companion object {
        private val RECEIVE_RE = Regex("""Bạn\s+nhận\s+[\d.,]+\s*[đĐ]""", RegexOption.IGNORE_CASE)
        private val SEND_RE    = Regex("""Chuyển\s+[\d.,]+\s*[đĐ]\s+đến""", RegexOption.IGNORE_CASE)
        private val PAYMENT_RE = Regex("""Thanh\s+toán\s+[\d.,]+\s*[đĐ]\s+(?:tại|cho)\s+(.+)""", RegexOption.IGNORE_CASE)
        private val AMOUNT_RE  = Regex("""([\d]{1,3}(?:[.,][\d]{3})*)(?:\s*)[đĐ]""", RegexOption.IGNORE_CASE)

        fun extractAmount(text: String): Double? =
            AMOUNT_RE.find(text)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.replace(".", "")
                ?.toDoubleOrNull()
    }
}
