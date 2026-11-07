package com.hativ2.web

import android.webkit.JavascriptInterface

/**
 * window.HatiAndroid, the only thing the APK adds to the page.
 *
 *  - HatiAndroid.share(text): opens the Android share sheet with plain text
 *    (stand-in for navigator.share({ text })).
 *  - HatiAndroid.saveFile(name, mime, text): opens the system "save as" dialog
 *    with [name] suggested and writes [text] to the file the user picks; a toast
 *    says "Saved" or "Couldn't save the file" (stand-in for <a download>).
 *
 * Both return immediately (no value). Calls are ignored unless the WebView is
 * showing the bundled page, so no other origin can use the bridge.
 */
class HatiBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun share(text: String?) = onOwnPage { activity.share(text.orEmpty()) }

    @JavascriptInterface
    fun saveFile(name: String?, mime: String?, text: String?) =
        onOwnPage { activity.saveFile(name.orEmpty().ifBlank { "hati2.json" }, mime.orEmpty(), text.orEmpty()) }

    // JavaScript calls arrive on a binder thread; the WebView and intents need the UI thread.
    private fun onOwnPage(action: () -> Unit) {
        activity.runOnUiThread { if (activity.isOwnPage()) action() }
    }

    companion object {
        const val NAME = "HatiAndroid"
    }
}
