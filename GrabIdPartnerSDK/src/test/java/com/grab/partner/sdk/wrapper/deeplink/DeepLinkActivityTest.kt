package com.grab.partner.sdk.wrapper.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import java.io.Serializable

private const val REDIRECT_URL = "grabpartner://callback?code=abc&state=xyz"

@RunWith(RobolectricTestRunner::class)
class DeepLinkActivityTest {

    private class AttackerPayload(val value: String) : Serializable

    private fun attackerIntent(action: String? = Intent.ACTION_VIEW, data: Uri? = Uri.parse(REDIRECT_URL)): Intent =
        Intent(action, data).apply {
            putExtra("attacker_string", "malicious")
            putExtra("attacker_serializable", AttackerPayload("gadget"))
            putExtra("attacker_bundle", Bundle().apply { putString("nested", "payload") })
        }

    private fun buildActivity(intent: Intent): ActivityController<DeepLinkActivity> {
        val controller = Robolectric.buildActivity(DeepLinkActivity::class.java, intent)
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        return controller
    }

    @Test
    fun `onCreate clears attacker-controlled extras from the launch intent`() {
        val intent = attackerIntent()

        val activity = buildActivity(intent).create().get()

        assertNull(intent.extras)
        assertNull(activity.intent.extras)
    }

    @Test
    fun `onCreate clears extras even when the intent is not a deep link`() {
        val intent = attackerIntent(action = Intent.ACTION_MAIN, data = null)

        val activity = buildActivity(intent).create().get()

        assertNull(intent.extras)
        assertNull(activity.intent.extras)
    }

    @Test
    fun `onCreate keeps action and data so the redirect is still handled`() {
        val activity = buildActivity(attackerIntent()).create().get()

        assertEquals(Intent.ACTION_VIEW, activity.intent.action)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `onNewIntent clears attacker-controlled extras from the new intent`() {
        val controller = buildActivity(Intent(Intent.ACTION_MAIN)).create().start().resume()
        val newIntent = attackerIntent()

        controller.newIntent(newIntent)

        assertNull(newIntent.extras)
        assertSame(newIntent, controller.get().intent)
        assertNull(controller.get().intent.extras)
        assertEquals(REDIRECT_URL, controller.get().intent.dataString)
    }

    @Test
    fun `onNewIntent clears extras even when the new intent is not a deep link`() {
        val controller = buildActivity(Intent(Intent.ACTION_MAIN)).create().start().resume()
        val newIntent = attackerIntent(action = Intent.ACTION_MAIN, data = null)

        controller.newIntent(newIntent)

        assertNull(newIntent.extras)
        assertNull(controller.get().intent.extras)
    }
}
