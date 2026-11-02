package com.usuallycoding.openadhd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.DesktopReminderScheduler
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The Alarm choice with the real desktop scheduler, which rings alarms and so offers the choice like a phone does. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DesktopAlarmKindUiTest {
    private val zone = TimeZone.currentSystemDefault()
    private val clock = FakeClock(Instant.parse("2026-10-23T09:00:00Z"))
    private val store = InMemoryStore(clock)
    private val today get() = clock.todayIn(zone)
    private val explanation = "An alarm rings where your device supports it; otherwise it is shown as a notification."

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun entry(): PlannedTask = runBlocking {
        store.assign(store.create("Clean the kitchen", Tier.Two, Energy.Low), today)
    }

    private fun entries(): List<PlannedTask> = runBlocking { store.entriesFor(today).first() }

    private fun androidx.compose.ui.test.ComposeUiTest.show() {
        val scheduler = DesktopReminderScheduler(notify = {}, scope = CoroutineScope(Job()))
        assertTrue(scheduler.supportsAlarms)
        setContent { App(AppContainer(store, clock = clock, dateCheckInterval = null, reminders = scheduler)) }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.reveal(text: String) {
        onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text))
    }

    private fun preset(label: String) = ReminderRules.presets(clock.now(), today, 22 * 60, zone).first { it.label == label }

    @Test
    fun `the entry sheet offers notification and alarm with the current kind selected`() = runComposeUiTest {
        entry()
        show()
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("Notification").assertIsSelected()
        onNodeWithText("Alarm").assertIsNotSelected()
        onNodeWithText(explanation).assertExists()
    }

    @Test
    fun `an alarm saved on the desktop is stored as an alarm and the row says so`() = runComposeUiTest {
        entry()
        show()
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()

        assertEquals(Reminder(preset("In 30 minutes").startAt, 30, ReminderKind.Alarm), entries().single().reminder)
        onNodeWithText("Alarm ${ReminderRules.clock(preset("In 30 minutes").startAt, zone)}, every 30 min").assertExists()
    }

    @Test
    fun `an entry with an alarm opens with alarm selected`() = runComposeUiTest {
        val e = entry()
        runBlocking { store.setReminder(e.id, Reminder(preset("In 1 hour").startAt, 60, ReminderKind.Alarm)) }
        show()
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("Alarm").assertIsSelected()
    }

    @Test
    fun `the template form offers the kind and keeps an alarm`() = runComposeUiTest {
        show()
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Meds")
        onNodeWithText("Set a time").performClick()
        onNodeWithText("OK").performClick()
        reveal("Alarm")
        onNodeWithText("Notification").assertIsSelected()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save").performClick()
        waitForIdle()

        assertEquals(ReminderKind.Alarm, runBlocking { store.templates.first().single().reminder?.kind })
    }
}
