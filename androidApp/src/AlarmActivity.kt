package com.usuallycoding.openadhd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.ui.theme.AppTheme

/** The screen of a ringing alarm, shown over the lock screen with the task's name, Open task and Dismiss. */
class AlarmActivity : ComponentActivity() {
    private var entryId by mutableStateOf("")
    private var text by mutableStateOf("")

    private val events = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AlarmService.ActionEnded -> finish()
                AlarmService.ActionRinging -> show(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )
        show(intent)
        val filter = IntentFilter().apply {
            addAction(AlarmService.ActionEnded)
            addAction(AlarmService.ActionRinging)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(events, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(events, filter)
        }
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Still to do", style = MaterialTheme.typography.titleMedium)
                        Text(text, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                        Button(onClick = ::openTask, modifier = Modifier.fillMaxWidth()) { Text("Open task") }
                        OutlinedButton(onClick = ::dismiss, modifier = Modifier.fillMaxWidth()) { Text("Dismiss") }
                    }
                }
            }
        }
    }

    /** A second alarm replaces the first one's text. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show(intent)
    }

    private fun show(intent: Intent) {
        intent.getStringExtra(ReminderAlarms.ExtraEntryId)?.let { entryId = it }
        intent.getStringExtra(ReminderAlarms.ExtraText)?.let { text = it }
    }

    private fun openTask() {
        startActivity(TaskIntents.openTaskIntent(this, entryId))
        startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ActionDismiss))
        finish()
    }

    private fun dismiss() {
        startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ActionDismiss))
        finish()
    }

    override fun onDestroy() {
        unregisterReceiver(events)
        super.onDestroy()
    }

    companion object {
        fun intent(context: Context, entryId: String, text: String): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(ReminderAlarms.ExtraEntryId, entryId)
                .putExtra(ReminderAlarms.ExtraText, text)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
