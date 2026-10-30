package com.usuallycoding.openadhd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.usuallycoding.openadhd.data.createAndroidContainer

class MainActivity : ComponentActivity() {
    private val container by lazy { createAndroidContainer(this, AndroidDataFiles(this), AndroidReminderScheduler(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App(container)
        }
        // A restored activity (a rotation, say) gets its original intent again; it was handled the first time.
        if (savedInstanceState == null) showEntryOf(intent)
    }

    /**
     * Whichever way the user comes to the app, a ringing alarm has been noticed: it ends, as with Dismiss, and later repeats still
     * ring. An alarm that starts while the app is already in front is not ended, because this runs when the app comes to the front.
     */
    override fun onStart() {
        super.onStart()
        if (AlarmService.ringing) {
            startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ActionDismiss))
        }
    }

    /** A tap on a reminder notification while the activity is already running. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showEntryOf(intent)
    }

    /** Asks the app to show the entry a tapped reminder notification is about, once. */
    private fun showEntryOf(intent: Intent?) {
        val entryId = intent?.getStringExtra(ReminderAlarms.ExtraEntryId) ?: return
        intent.removeExtra(ReminderAlarms.ExtraEntryId)
        // Opening the app from a ringing alarm's notification ends the ring.
        if (intent.getBooleanExtra(ReminderAlarms.ExtraStopAlarm, false)) {
            intent.removeExtra(ReminderAlarms.ExtraStopAlarm)
            startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ActionDismiss))
        }
        container.entryRequests.open(entryId)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
