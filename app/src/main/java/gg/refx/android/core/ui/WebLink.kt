package gg.refx.android.core.ui

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Opens an **https-only** URL in a Chrome Custom Tab. Mirrors the iOS `WebLink`
 * guard: server-provided URLs that aren't https are refused, so we never launch
 * arbitrary schemes (§9).
 */
object WebLink {
    fun open(context: Context, url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
        return true
    }
}
