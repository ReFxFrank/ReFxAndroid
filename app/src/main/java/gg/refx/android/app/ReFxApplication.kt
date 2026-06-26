package gg.refx.android.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import gg.refx.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ReFxApplication : Application() {

    // App-scoped coroutine scope for long-lived collectors (config, session).
    val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)
        createDefaultNotificationChannel()
    }

    private fun createDefaultNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            "ReFx notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Server, billing and support alerts"
        }
        manager.createNotificationChannel(channel)
    }
}
