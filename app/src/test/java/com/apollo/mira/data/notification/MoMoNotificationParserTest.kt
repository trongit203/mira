package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class MoMoNotificationParserTest {

    private val parser = MoMoNotificationParser()
    private val ts = 1_700_000_000_000L

    @Test
    fun `canHandle returns true for MoMo package`() {
        assertTrue(parser.canHandle("com.mservice.momotransfer"))
    }

    @Test
    fun `canHandle returns false for other packages`() {
        assertFalse(parser.canHandle("vn.com.vng.zalopay"))
        assertFalse(parser.canHandle("com.VCB"))
        assertFalse(parser.canHandle("com.facebook.katana"))
    }

    @Test
    fun `receive pattern returns INCOME transaction`() {
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            text = "Bạn nhận 150.000đ từ Nguyễn Văn A",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.INCOME, tx!!.type)
        assertEquals(150_000.0, tx.amount, 0.0)
        assertEquals(DataSource.MOMO, tx.source)
        assertEquals(ts, tx.timestamp)
    }

    @Test
    fun `send pattern returns EXPENSE with Chuyen khoan category`() {
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            text = "Chuyển 200.000đ đến Trần Thị B thành công",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.EXPENSE, tx!!.type)
        assertEquals(200_000.0, tx.amount, 0.0)
        assertEquals("Chuyển khoản", tx.category)
        assertEquals(DataSource.MOMO, tx.source)
    }

    @Test
    fun `payment at cafe infers An uong category`() {
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            text = "Thanh toán 55.000đ tại Highlands Coffee",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.EXPENSE, tx!!.type)
        assertEquals(55_000.0, tx.amount, 0.0)
        assertEquals("Ăn uống", tx.category)
    }

    @Test
    fun `payment at generic merchant defaults to Khac`() {
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            text = "Thanh toán 30.000đ tại Cửa hàng XYZ",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals("Khác", tx!!.category)
    }

    @Test
    fun `unknown pattern returns null`() {
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            text = "Khuyến mãi tháng 4: hoàn 10% cho mọi giao dịch",
            timestamp = ts
        )
        assertNull(tx)
    }

    @Test
    fun `text without amount returns null`() {
        val tx = parser.parse(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            text = "Bạn nhận tiền từ Nguyễn Văn A",
            timestamp = ts
        )
        assertNull(tx)
    }

    @Test
    fun `extractAmount handles dot separator format`() {
        assertEquals(1_500_000.0, MoMoNotificationParser.extractAmount("nhận 1.500.000đ hôm nay")!!, 0.0)
    }

    @Test
    fun `extractAmount handles comma separator format`() {
        assertEquals(500_000.0, MoMoNotificationParser.extractAmount("chuyển 500,000đ")!!, 0.0)
    }
}
