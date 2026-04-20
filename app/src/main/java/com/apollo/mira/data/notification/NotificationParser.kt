package com.apollo.mira.data.notification

import com.apollo.mira.domain.model.Transaction

interface NotificationParser {
    fun canHandle(packageName: String): Boolean
    fun parse(packageName: String, title: String, text: String, timestamp: Long): Transaction?
}
