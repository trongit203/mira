package com.apollo.mira.data.notification

import org.junit.Assert.*
import org.junit.Test

class CategoryInferenceEngineTest {

    @Test
    fun `cafe keyword maps to An uong`() {
        assertEquals("Ăn uống", CategoryInferenceEngine.inferExpenseCategory("Highlands Coffee"))
    }

    @Test
    fun `an keyword maps to An uong`() {
        assertEquals("Ăn uống", CategoryInferenceEngine.inferExpenseCategory("Quán ăn Ngon"))
    }

    @Test
    fun `grab keyword maps to Di chuyen`() {
        assertEquals("Di chuyển", CategoryInferenceEngine.inferExpenseCategory("Grab taxi"))
    }

    @Test
    fun `dien keyword maps to Hoa don`() {
        assertEquals("Hóa đơn", CategoryInferenceEngine.inferExpenseCategory("Tiền điện EVN"))
    }

    @Test
    fun `bao hiem keyword maps to Bao hiem`() {
        assertEquals("Bảo hiểm", CategoryInferenceEngine.inferExpenseCategory("Phí bảo hiểm Prudential"))
    }

    @Test
    fun `unknown text defaults to Khac`() {
        assertEquals("Khác", CategoryInferenceEngine.inferExpenseCategory("Cửa hàng tạp hóa"))
    }

    @Test
    fun `nhan keyword maps to Thu nhap income`() {
        assertEquals("Thu nhập", CategoryInferenceEngine.inferIncomeCategory("Bạn nhận tiền từ công ty"))
    }

    @Test
    fun `hoan keyword maps to Hoan tien`() {
        assertEquals("Hoàn tiền", CategoryInferenceEngine.inferIncomeCategory("Hoàn tiền đơn hàng Shopee"))
    }

    @Test
    fun `unknown income text defaults to Thu nhap`() {
        assertEquals("Thu nhập", CategoryInferenceEngine.inferIncomeCategory("Giao dịch thành công"))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals("Ăn uống", CategoryInferenceEngine.inferExpenseCategory("CAFE AMAZON"))
    }
}
