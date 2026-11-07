package com.hativ2.web

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Runs the real bundled web app in the shell on an emulator. */
@RunWith(AndroidJUnit4::class)
class WebShellTest {

    private fun launch(uri: String? = null): ActivityScenario<MainActivity> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(ctx, MainActivity::class.java)
        if (uri != null) intent.setAction(Intent.ACTION_VIEW).setData(Uri.parse(uri))
        return ActivityScenario.launch(intent)
    }

    private fun js(scenario: ActivityScenario<MainActivity>, script: String): String {
        var out = ""
        val latch = CountDownLatch(1)
        scenario.onActivity { a -> a.webView.evaluateJavascript(script) { out = it; latch.countDown() } }
        latch.await(10, TimeUnit.SECONDS)
        return out
    }

    /** Polls until the app has rendered (#app has content) or 30 s pass. */
    private fun awaitApp(scenario: ActivityScenario<MainActivity>) {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            if (js(scenario, "!!(document.querySelector('#app') && document.querySelector('#app').children.length)") == "true") return
            Thread.sleep(250)
        }
        throw AssertionError("web app did not render; url=" + js(scenario, "location.href"))
    }

    @Test
    fun bundledAppRendersWithStorageAndBridge() {
        launch().use { s ->
            awaitApp(s)
            assertEquals("\"https://appassets.androidplatform.net\"", js(s, "location.origin"))
            assertEquals("true", js(s, "window.isSecureContext"))
            assertEquals("\"1\"", js(s, "localStorage.setItem('hati2.apk.test','1'); localStorage.getItem('hati2.apk.test')"))
            assertEquals("\"function\"", js(s, "typeof window.HatiAndroid.share"))
            assertEquals("\"function\"", js(s, "typeof window.HatiAndroid.saveFile"))
        }
    }

    @Test
    fun inviteLinkFragmentReachesThePage() {
        launch("https://p1xellzxc.github.io/HATI_v2/app/#join=abc").use { s ->
            awaitApp(s)
            assertEquals("\"/assets/app/index.html\"", js(s, "location.pathname"))
        }
    }

    @Test
    fun startUrlMapping() {
        val index = MainActivity.INDEX
        assertEquals("$index#join=abc", MainActivity.startUrl(Uri.parse("https://p1xellzxc.github.io/HATI_v2/app/#join=abc")))
        assertEquals("$index#v1.xyz", MainActivity.startUrl(Uri.parse("hati2://join#v1.xyz")))
        assertEquals(index, MainActivity.startUrl(Uri.parse("hati2://join")))
        assertEquals(index, MainActivity.startUrl(Uri.parse("https://example.com/#evil")))
        assertEquals(index, MainActivity.startUrl(null))
    }
}
