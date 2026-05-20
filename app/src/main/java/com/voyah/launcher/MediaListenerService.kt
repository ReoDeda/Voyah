package com.voyah.launcher

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MediaListenerService : NotificationListenerService() {
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        val intent = Intent("com.voyah.launcher.MEDIA_LISTENER_CONNECTED")
        sendBroadcast(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // Обновляем плеер, если пришло уведомление от медиаплеера
        if (sbn?.notification?.extras?.containsKey("android.mediaSession") == true) {
            val intent = Intent("com.voyah.launcher.MEDIA_UPDATED")
            sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.notification?.extras?.containsKey("android.mediaSession") == true) {
            val intent = Intent("com.voyah.launcher.MEDIA_UPDATED")
            sendBroadcast(intent)
        }
    }
}