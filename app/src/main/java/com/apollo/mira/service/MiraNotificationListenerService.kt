package com.apollo.mira.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.apollo.mira.data.notification.NotificationParserFactory
import com.apollo.mira.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineDispatcher

@AndroidEntryPoint
class MiraNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var addTransactionUseCase: AddTransactionUseCase
    @Inject lateinit var parserFactory: NotificationParserFactory
    @Inject @Named("IO") lateinit var ioDispatcher: CoroutineDispatcher

    private val serviceJob = SupervisorJob()
    private val serviceScope by lazy { CoroutineScope(serviceJob + ioDispatcher) }

    private val processedKeys = mutableSetOf<String>()
    private val MAX_CACHE = 200

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        parserFactory.findParser(pkg) ?: return

        val key = sbn.key
        if (key in processedKeys) return
        if (processedKeys.size >= MAX_CACHE) processedKeys.clear()
        processedKeys.add(key)

        val extras = sbn.notification.extras
        val title  = extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text   = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank()) return

        val transaction = parserFactory.findParser(pkg)!!.parse(pkg, title, text, sbn.postTime) ?: return

        serviceScope.launch {
            runCatching { addTransactionUseCase(transaction) }
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
