package gg.refx.android.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import gg.refx.android.core.design.ReFxTheme
import gg.refx.android.core.design.screenBackground
import gg.refx.android.core.push.PushRouter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as ReFxApplication).container

        // Cold launch: a tapped-notification intent exists before the UI does —
        // seed the router so the nav host applies it on first composition (§7).
        container.pushRouter.submit(routeFromIntent(intent))

        setContent {
            ReFxTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    NotificationPermissionGate()
                    Box(Modifier.fillMaxSize().screenBackground()) {
                        RootContent()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val container = (application as ReFxApplication).container
        container.pushRouter.submit(routeFromIntent(intent))
    }

    private fun routeFromIntent(intent: Intent?): gg.refx.android.core.push.PendingRoute? {
        intent ?: return null
        val data = mapOf(
            PushRouter.KEY_TYPE to intent.getStringExtra(PushRouter.KEY_TYPE),
            PushRouter.KEY_SERVER_ID to intent.getStringExtra(PushRouter.KEY_SERVER_ID),
            PushRouter.KEY_INVOICE_ID to intent.getStringExtra(PushRouter.KEY_INVOICE_ID),
            PushRouter.KEY_TICKET_ID to intent.getStringExtra(PushRouter.KEY_TICKET_ID),
        )
        if (data.values.all { it == null }) return null
        return PushRouter.fromData(data)
    }
}

@androidx.compose.runtime.Composable
private fun NotificationPermissionGate() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored; users can change it later in settings */ }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
