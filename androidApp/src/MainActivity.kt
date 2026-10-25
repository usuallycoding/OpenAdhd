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
        container.entryRequests.open(entryId)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
