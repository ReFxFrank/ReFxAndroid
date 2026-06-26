package gg.refx.android.app.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import gg.refx.android.R
import gg.refx.android.app.MainActivity
import gg.refx.android.app.ReFxApplication
import gg.refx.android.core.push.PushRouter

/**
 * FCM entry point (§7). Registers token refreshes and renders incoming messages as
 * notifications whose tap deep-links into the right tab + entity (via intent
 * extras consumed by [MainActivity]). Backend notification types: `server.state`,
 * `billing.invoice`, `support.reply`.
 */
class ReFxMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        (application as? ReFxApplication)?.container?.pushRegistrar?.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "ReFx"
        val body = message.notification?.body ?: data["body"] ?: ""
        showNotification(title, body, data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            data[PushRouter.KEY_TYPE]?.let { putExtra(PushRouter.KEY_TYPE, it) }
            data[PushRouter.KEY_SERVER_ID]?.let { putExtra(PushRouter.KEY_SERVER_ID, it) }
            data[PushRouter.KEY_INVOICE_ID]?.let { putExtra(PushRouter.KEY_INVOICE_ID, it) }
            data[PushRouter.KEY_TICKET_ID]?.let { putExtra(PushRouter.KEY_TICKET_ID, it) }
        }
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val contentIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_stat_refx)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(requestCode, notification)
        }
    }
}
