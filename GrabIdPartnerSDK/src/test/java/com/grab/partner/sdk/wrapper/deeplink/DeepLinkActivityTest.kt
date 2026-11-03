/*
 * Copyright (c) Grab Taxi Holdings PTE LTD (GRAB)
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 */

package com.grab.partner.sdk.wrapper.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

private const val REDIRECT_URL = "grabweblogin://open?code=abc&state=xyz"

@RunWith(RobolectricTestRunner::class)
class DeepLinkActivityTest {

    @Test
    fun `onCreate clears all extras from the launch intent`() {
        val intent = intentWithAttackerExtras(Intent.ACTION_MAIN)

        val activity = buildActivity(intent).create().get()

        assertNull(intent.extras)
        assertNull(activity.intent.extras)
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `onCreate clears extras from a deep link but keeps action and data`() {
        val intent = intentWithAttackerExtras(Intent.ACTION_VIEW, Uri.parse(REDIRECT_URL))

        val activity = buildActivity(intent).create().get()

        assertNull(activity.intent.extras)
        assertEquals(Intent.ACTION_VIEW, activity.intent.action)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `onNewIntent clears all extras from the new intent`() {
        val controller = buildActivity(Intent(Intent.ACTION_MAIN)).create()
        val newIntent = intentWithAttackerExtras(Intent.ACTION_MAIN)

        controller.newIntent(newIntent)

        assertNull(newIntent.extras)
        assertNull(controller.get().intent.extras)
    }

    @Test
    fun `onNewIntent clears extras from a deep link but keeps action and data`() {
        val controller = buildActivity(Intent(Intent.ACTION_MAIN)).create()
        val newIntent = intentWithAttackerExtras(Intent.ACTION_VIEW, Uri.parse(REDIRECT_URL))

        controller.newIntent(newIntent)

        val activity = controller.get()
        assertNull(activity.intent.extras)
        assertEquals(Intent.ACTION_VIEW, activity.intent.action)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
    }

    private fun buildActivity(intent: Intent): ActivityController<DeepLinkActivity> =
        Robolectric.buildActivity(DeepLinkActivity::class.java, intent).also {
            // DeepLinkActivity is an AppCompatActivity but the SDK manifest doesn't declare it with a theme.
            it.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        }

    private fun intentWithAttackerExtras(action: String, data: Uri? = null): Intent =
        Intent(action, data).apply {
            putExtra("attacker_string", "payload")
            putExtra("attacker_bundle", Bundle().apply { putString("nested", "payload") })
        }
}
