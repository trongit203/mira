package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.DataSource
import com.apollo.mira.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class BankNotificationParserTest {

    private val parser = BankNotificationParser()
    private val ts = 1_700_000_000_000L

    @Test
    fun `canHandle returns true for known bank packages`() {
        assertTrue(parser.canHandle("com.VCB"))
        assertTrue(parser.canHandle("com.mbmobile"))
        assertTrue(parser.canHandle("com.bidv.smartbanking"))
        assertTrue(parser.canHandle("com.techcombank.mb.android"))
    }

    @Test
    fun `canHandle returns false for non-bank packages`() {
        assertFalse(parser.canHandle("com.mservice.momotransfer"))
        assertFalse(parser.canHandle("vn.com.vng.zalopay"))
        assertFalse(parser.canHandle("com.facebook.katana"))
    }

    @Test
    fun `credit notification returns INCOME transaction`() {
        val tx = parser.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "Tài khoản của bạn ghi có 2.000.000đ. Số dư tăng lên 5.500.000đ",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.INCOME, tx!!.type)
        assertEquals(DataSource.BANK_SYNC, tx.source)
    }

    @Test
    fun `debit notification returns EXPENSE transaction`() {
        val tx = parser.parse(
            packageName = "com.mbmobile",
            title = "MB Bank",
            text = "Ghi nợ 350.000đ. Thanh toán hóa đơn điện. Số dư giảm còn 1.200.000đ",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals(TransactionType.EXPENSE, tx!!.type)
        assertEquals(DataSource.BANK_SYNC, tx.source)
    }

    @Test
    fun `debit for electricity infers Hoa don category`() {
        val tx = parser.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "Trừ tiền 250.000đ thanh toán tiền điện EVN",
            timestamp = ts
        )
        assertNotNull(tx)
        assertEquals("Hóa đơn", tx!!.category)
    }

    @Test
    fun `text without amount returns null`() {
        val tx = parser.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "Đăng nhập thành công vào tài khoản",
            timestamp = ts
        )
        assertNull(tx)
    }

    @Test
    fun `ambiguous text without credit or debit keyword returns null`() {
        val tx = parser.parse(
            packageName = "com.VCB",
            title = "Vietcombank",
            text = "Số dư tài khoản: 10.000.000đ",
            timestamp = ts
        )
        assertNull(tx)
    }
}
