package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType

class ZaloPayNotificationParser : NotificationParser {

    override fun canHandle(packageName: String): Boolean =
        packageName == "vn.com.vng.zalopay"

    override fun parse(packageName: String, title: String, text: String, timestamp: Long): Transaction? {
        val amount = extractAmount(text) ?: return null
        return when {
            RECEIVE_RE.containsMatchIn(text) -> Transaction(
                amount    = amount,
                category  = CategoryInferenceEngine.inferIncomeCategory(text),
                note      = text.trim(),
                type      = TransactionType.INCOME,
                timestamp = timestamp,
                source    = DataSource.ZALOPAY
            )
            PAYMENT_RE.containsMatchIn(text) -> Transaction(
                amount    = amount,
                category  = CategoryInferenceEngine.inferExpenseCategory(text),
                note      = text.trim(),
                type      = TransactionType.EXPENSE,
                timestamp = timestamp,
                source    = DataSource.ZALOPAY
            )
            SENT_RE.containsMatchIn(text) -> Transaction(
                amount    = amount,
                category  = "Chuyển khoản",
                note      = text.trim(),
                type      = TransactionType.EXPENSE,
                timestamp = timestamp,
                source    = DataSource.ZALOPAY
            )
            else -> null
        }
    }

    companion object {
        private val RECEIVE_RE = Regex("""vừa\s+nhận""", RegexOption.IGNORE_CASE)
        private val SENT_RE    = Regex("""giao\s+dịch.+thành\s+công""", RegexOption.IGNORE_CASE)
        private val PAYMENT_RE = Regex("""thanh\s+toán""", RegexOption.IGNORE_CASE)
        private val AMOUNT_RE  = Regex("""([\d]{1,3}(?:[.,][\d]{3})*)(?:\s*)[đĐ]""", RegexOption.IGNORE_CASE)

        fun extractAmount(text: String): Double? =
            AMOUNT_RE.find(text)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.replace(".", "")
                ?.toDoubleOrNull()
    }
}
