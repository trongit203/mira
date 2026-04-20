package com.apollo.mira.data.notification

object CategoryInferenceEngine {

    private val EXPENSE_KEYWORDS: Map<String, String> = mapOf(
        "ăn"         to "Ăn uống",
        "cafe"       to "Ăn uống",
        "cà phê"     to "Ăn uống",
        "trà"        to "Ăn uống",
        "coffee"     to "Ăn uống",
        "food"       to "Ăn uống",
        "grab"       to "Di chuyển",
        "uber"       to "Di chuyển",
        "be "        to "Di chuyển",
        "xe"         to "Di chuyển",
        "điện"       to "Hóa đơn",
        "nước"       to "Hóa đơn",
        "internet"   to "Hóa đơn",
        "điện thoại" to "Hóa đơn",
        "viễn thông" to "Hóa đơn",
        "bảo hiểm"   to "Bảo hiểm",
        "siêu thị"   to "Mua sắm",
        "shop"       to "Mua sắm",
    )

    private val INCOME_KEYWORDS: Map<String, String> = mapOf(
        "nhận"   to "Thu nhập",
        "lương"  to "Thu nhập",
        "hoàn"   to "Hoàn tiền",
        "refund" to "Hoàn tiền",
        "thưởng" to "Thu nhập",
    )

    fun inferExpenseCategory(text: String): String {
        val lower = text.lowercase()
        return EXPENSE_KEYWORDS.entries.firstOrNull { lower.contains(it.key) }?.value ?: "Khác"
    }

    fun inferIncomeCategory(text: String): String {
        val lower = text.lowercase()
        return INCOME_KEYWORDS.entries.firstOrNull { lower.contains(it.key) }?.value ?: "Thu nhập"
    }
}
