package com.apollo.mira.data.notification

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationParserFactory @Inject constructor() {

    private val parsers: List<NotificationParser> = listOf(
        MoMoNotificationParser(),
        ZaloPayNotificationParser(),
        BankNotificationParser(),
    )

    fun findParser(packageName: String): NotificationParser? =
        parsers.firstOrNull { it.canHandle(packageName) }
}
