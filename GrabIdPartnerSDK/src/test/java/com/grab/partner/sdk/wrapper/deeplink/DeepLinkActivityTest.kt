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
import com.grab.partner.sdk.models.LoginSession
import com.grab.partner.sdk.utils.Utility
import com.grab.partner.sdk.wrapper.manager.GrabSdkManager
import com.grab.partner.sdk.wrapper.manager.GrabSdkManagerImpl
import com.grab.partner.sdk.wrapper.manager.SessionCallbacks
import java.util.concurrent.ConcurrentHashMap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class DeepLinkActivityTest {
    private var controller: ActivityController<DeepLinkActivity>? = null
    private val sessionCallbacks: SessionCallbacks = mock()
    private val sessions = ConcurrentHashMap<String, GrabSdkManager.Builder>()

    @Before
    fun setUp() {
        sessions[STATE] = GrabSdkManager.Builder().apply {
            listener = sessionCallbacks
            exchangeRequired = false
            loginSession = LoginSession()
        }
        GrabSdkManagerImpl.getInstance().apply {
            utility = Utility()
            sessions = this@DeepLinkActivityTest.sessions
        }
        setManagerInitialized(true)
    }

    @After
    fun tearDown() {
        controller?.destroy()
        sessions.clear()
        setManagerInitialized(false)
    }

    @Test
    fun `onCreate clears attacker-controlled extras`() {
        val activity = launch(intentWithAttackerExtras(Intent.ACTION_MAIN, null))

        assertNull(activity.intent.extras)
    }

    @Test
    fun `onCreate clears extras on a redirect deep link and still completes the flow`() {
        val intent = intentWithAttackerExtras(Intent.ACTION_VIEW, Uri.parse(REDIRECT_URL))

        val activity = launch(intent)

        assertNull(activity.intent.extras)
        assertNull(intent.extras)
        assertEquals(Intent.ACTION_VIEW, activity.intent.action)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
        assertLoginCallbackReceivedCode()
    }

    @Test
    fun `onNewIntent clears attacker-controlled extras`() {
        val activity = launch(Intent(Intent.ACTION_MAIN))
        val newIntent = intentWithAttackerExtras(Intent.ACTION_MAIN, null)

        controller!!.newIntent(newIntent)

        assertNull(newIntent.extras)
        assertNull(activity.intent.extras)
    }

    @Test
    fun `onNewIntent clears extras on a redirect deep link and keeps action and data`() {
        val activity = launch(Intent(Intent.ACTION_MAIN))
        val newIntent = intentWithAttackerExtras(Intent.ACTION_VIEW, Uri.parse(REDIRECT_URL))

        controller!!.newIntent(newIntent)

        assertNull(activity.intent.extras)
        assertEquals(Intent.ACTION_VIEW, activity.intent.action)
        assertEquals(REDIRECT_URL, activity.intent.dataString)
        assertTrue(activity.isFinishing)
        assertLoginCallbackReceivedCode()
    }

    private fun launch(intent: Intent): DeepLinkActivity {
        val activityController = Robolectric.buildActivity(DeepLinkActivity::class.java, intent)
        controller = activityController
        activityController.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        return activityController.create().get()
    }

    private fun assertLoginCallbackReceivedCode() {
        val captor = argumentCaptor<LoginSession>()
        verify(sessionCallbacks).onSuccess(captor.capture())
        assertEquals(CODE, captor.firstValue.code)
    }

    private fun setManagerInitialized(initialized: Boolean) {
        GrabSdkManagerImpl::class.java.getDeclaredField("initInvoked").apply {
            isAccessible = true
            setBoolean(null, initialized)
        }
    }

    private fun intentWithAttackerExtras(action: String, data: Uri?): Intent =
        Intent(action, data).apply {
            putExtra("attacker_string", "payload")
            putExtra("attacker_parcelable", Uri.parse("https://attacker.example"))
            putExtra("attacker_bundle", Bundle().apply { putString("nested", "payload") })
        }

    private companion object {
        const val CODE = "CODE"
        const val STATE = "STATE"
        const val REDIRECT_URL = "grabidsdk://open?code=$CODE&state=$STATE"
    }
}
