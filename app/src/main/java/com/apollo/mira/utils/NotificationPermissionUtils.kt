package com.apollo.mira.utils

import android.content.Context
import androidx.core.app.NotificationManagerCompat

object NotificationPermissionUtils {
    fun isNotificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}