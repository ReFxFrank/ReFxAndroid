package gg.refx.android.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM entry point (§7). Milestone-1 stub: receives token refreshes and messages.
 *
 * Milestone 3 completes this:
 *  - register the token via `POST account/push-tokens` on the signed-in transition
 *    (not before auth — the iOS bug to avoid, §7),
 *  - foreground banner + unread badge,
 *  - deep-link router surviving cold launch (`type` + entity id → tab + entity).
 */
class ReFxMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO(Milestone 3): persist and, if signed in, re-register with the backend.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // TODO(Milestone 3): build a deep-link route from message.data["type"] + id,
        // show a notification, and update the Account unread badge.
    }
}
