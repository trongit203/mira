package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class ZaloPayNotificationParserTest {

    private val parser = ZaloPayNotificationParser()
    private val ts = 1_700_000_000_000L

    @Test
    fun `canHandle returns true for ZaloPay package`() {
        assertTrue(parser.canHandle("vn.com.vng.zalopay"))
    }

    @Test
    fun `canHandle returns false for other packages`() {
        assertFalse(parser.canHandle("com.mservice.momotransfer"))
        assertFalse(parser.canHandle("com.VCB"))
    }

    @Test
    fun `receive pattern returns INCOME transaction`() {
        val tx = parser.parse(
            packageName = "vn.com.vng.zalopay",
            title = "ZaloPay",
            text = "Bạn vừa nhận 300.000đ từ Lê Văn C",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.INCOME, tx!!.type)
        assertEquals(300_000.0, tx.amount, 0.0)
        assertEquals(DataSource.ZALOPAY, tx.source)
    }

    @Test
    fun `payment pattern returns EXPENSE transaction`() {
        val tx = parser.parse(
            packageName = "vn.com.vng.zalopay",
            title = "ZaloPay",
            text = "Thanh toán 120.000đ tại Bún bò Huế thành công",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.EXPENSE, tx!!.type)
        assertEquals(120_000.0, tx.amount, 0.0)
        assertEquals(DataSource.ZALOPAY, tx.source)
    }

    @Test
    fun `send pattern returns EXPENSE with Chuyen khoan category`() {
        val tx = parser.parse(
            packageName = "vn.com.vng.zalopay",
            title = "ZaloPay",
            text = "Giao dịch 500.000đ đến Phạm Thị D thành công",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.EXPENSE, tx!!.type)
        assertEquals("Chuyển khoản", tx!!.category)
    }

    @Test
    fun `unknown text returns null`() {
        val tx = parser.parse(
            packageName = "vn.com.vng.zalopay",
            title = "ZaloPay",
            text = "Cập nhật ứng dụng mới nhất để nhận ưu đãi",
            timestamp = ts
        )
        assertNull(tx)
    }
}
