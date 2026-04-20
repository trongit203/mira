package com.apollo.mira.data.notification

import org.junit.Assert.*
import org.junit.Test

class NotificationParserFactoryTest {

    private val factory = NotificationParserFactory()

    @Test
    fun `MoMo package routes to MoMoNotificationParser`() {
        val parser = factory.findParser("com.mservice.momotransfer")
        assertNotNull(parser)
        assertTrue(parser is MoMoNotificationParser)
    }

    @Test
    fun `ZaloPay package routes to ZaloPayNotificationParser`() {
        val parser = factory.findParser("vn.com.vng.zalopay")
        assertNotNull(parser)
        assertTrue(parser is ZaloPayNotificationParser)
    }

    @Test
    fun `bank package routes to BankNotificationParser`() {
        val parser = factory.findParser("com.VCB")
        assertNotNull(parser)
        assertTrue(parser is BankNotificationParser)

        val parser2 = factory.findParser("com.mbmobile")
        assertNotNull(parser2)
        assertTrue(parser2 is BankNotificationParser)
    }

    @Test
    fun `unknown package returns null`() {
        assertNull(factory.findParser("com.facebook.katana"))
        assertNull(factory.findParser("com.google.android.gm"))
        assertNull(factory.findParser(""))
    }
}
