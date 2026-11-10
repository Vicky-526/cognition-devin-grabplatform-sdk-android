package com.grab.partner.sdk.wrapper.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.R
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class DeepLinkActivityTest {

    @Test
    fun `onCreate clears attacker controlled extras`() {
        val launchIntent = intentWithAttackerExtras()

        val controller = launch(launchIntent)

        assertNull(controller.get().intent.extras)
        assertNull(launchIntent.extras)
    }

    @Test
    fun `onCreate clears extras on a deep link redirect and still handles the redirect`() {
        val launchIntent = intentWithAttackerExtras(Intent.ACTION_VIEW, REDIRECT_URL)

        val activity = launch(launchIntent).get()

        assertNull(activity.intent.extras)
        assertEquals(Intent.ACTION_VIEW, activity.intent.action)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `onNewIntent clears attacker controlled extras`() {
        val controller = launch(Intent(ApplicationProvider.getApplicationContext(), DeepLinkActivity::class.java))
        val newIntent = intentWithAttackerExtras()

        controller.newIntent(newIntent)

        assertNull(newIntent.extras)
        assertNull(controller.get().intent.extras)
    }

    @Test
    fun `onNewIntent clears extras on a deep link redirect and still handles the redirect`() {
        val controller = launch(Intent(ApplicationProvider.getApplicationContext(), DeepLinkActivity::class.java))
        val newIntent = intentWithAttackerExtras(Intent.ACTION_VIEW, REDIRECT_URL)

        controller.newIntent(newIntent)

        val activity = controller.get()
        assertNull(activity.intent.extras)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
    }

    private fun launch(intent: Intent): ActivityController<DeepLinkActivity> {
        val controller = Robolectric.buildActivity(DeepLinkActivity::class.java, intent)
        controller.get().setTheme(R.style.Theme_AppCompat)
        return controller.create()
    }

    private fun intentWithAttackerExtras(action: String? = null, data: String? = null): Intent =
        Intent(ApplicationProvider.getApplicationContext(), DeepLinkActivity::class.java).apply {
            this.action = action
            data?.let { this.data = Uri.parse(it) }
            putExtra("attacker_string", "malicious")
            putExtra("attacker_bundle", Bundle().apply { putString("nested", "payload") })
            putExtra("attacker_serializable", arrayListOf("a", "b"))
        }

    private companion object {
        const val REDIRECT_URL = "grabpartner://callback?code=abc&state=xyz"
    }
}
