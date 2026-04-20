package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.Transaction
import com.apollo.mira.domain.model.TransactionType

class BankNotificationParser : NotificationParser {

    override fun canHandle(packageName: String): Boolean = packageName in BANK_PACKAGES

    override fun parse(packageName: String, title: String, text: String, timestamp: Long): Transaction? {
        val amount = extractAmount(text) ?: return null
        val type = when {
            CREDIT_RE.containsMatchIn(text) -> TransactionType.INCOME
            DEBIT_RE.containsMatchIn(text)  -> TransactionType.EXPENSE
            else -> return null
        }
        val category = if (type == TransactionType.INCOME)
            CategoryInferenceEngine.inferIncomeCategory(text)
        else
            CategoryInferenceEngine.inferExpenseCategory(text)

        return Transaction(
            amount    = amount,
            category  = category,
            note      = text.trim(),
            type      = type,
            timestamp = timestamp,
            source    = DataSource.BANK_SYNC
        )
    }

    companion object {
        val BANK_PACKAGES: Set<String> = setOf(
            "com.VCB",                     // Vietcombank
            "com.mbmobile",                // MB Bank
            "com.VietinBankiPay",          // Vietinbank iPay
            "com.bidv.smartbanking",       // BIDV
            "com.vietinbank.ipay",         // Vietinbank (alternate)
            "vn.vnpay.merchants",          // VNPay
            "com.tpb.mb.gprsandroid",      // TPBank
            "vn.agribank.mbplus",          // Agribank
            "com.techcombank.mb.android",  // Techcombank
            "com.acb.mobile",              // ACB
        )

        private val CREDIT_RE = Regex(
            """(?:số\s+dư\s+tăng|ghi\s+có|credit|nhận\s+được|\+\s*[\d]|cộng\s+tiền)""",
            RegexOption.IGNORE_CASE
        )
        private val DEBIT_RE = Regex(
            """(?:số\s+dư\s+giảm|ghi\s+nợ|debit|thanh\s+toán|rút\s+tiền|-\s*[\d]|trừ\s+tiền)""",
            RegexOption.IGNORE_CASE
        )
        private val AMOUNT_RE = Regex("""([\d]{1,3}(?:[.,][\d]{3})*)(?:\s*)[đĐ]""", RegexOption.IGNORE_CASE)

        fun extractAmount(text: String): Double? =
            AMOUNT_RE.find(text)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.replace(".", "")
                ?.toDoubleOrNull()
    }
}
