package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.ReminderKind
import com.usuallycoding.openadhd.domain.ReminderRules
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNotNull
import kotlin.time.Instant

/** Choosing Notification or Alarm for a task's reminder and for a template's. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AlarmKindUiTest {
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

    private fun ComposeUiTest.show(alarms: Boolean, fullScreenAllowed: Boolean = true, scheduler: FakeReminderScheduler = FakeReminderScheduler(supportsAlarms = alarms, fullScreenAllowed = fullScreenAllowed)) {
        setContent { App(AppContainer(store, clock = clock, dateCheckInterval = null, reminders = scheduler)) }
    }

    /** The template form is taller than the window, so bring a control into view before using it. */
    private fun ComposeUiTest.reveal(text: String) {
        onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText(text))
    }

    private fun preset(label: String) = ReminderRules.presets(clock.now(), today, 22 * 60, zone).first { it.label == label }

    @Test
    fun `a phone offers notification and alarm with an explanation`() = runComposeUiTest {
        entry()
        show(alarms = true)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("Notification").assertIsSelected()
        onNodeWithText("Alarm").assertIsNotSelected()
        onNodeWithText(explanation).assertExists()
    }

    @Test
    fun `a platform without alarms offers no choice`() = runComposeUiTest {
        entry()
        show(alarms = false)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("Alarm").assertDoesNotExist()
        onNodeWithText("Notification").assertDoesNotExist()
        onNodeWithText("Save reminder").assertExists()
    }

    @Test
    fun `a reminder is a notification unless alarm is chosen`() = runComposeUiTest {
        entry()
        show(alarms = true)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        assertEquals(Reminder(preset("In 30 minutes").startAt, 30, ReminderKind.Notification), entries().single().reminder)
        onNodeWithText("Reminder ${ReminderRules.clock(preset("In 30 minutes").startAt, zone)}, every 30 min").assertExists()
    }

    @Test
    fun `choosing alarm saves an alarm and the row says so`() = runComposeUiTest {
        entry()
        show(alarms = true)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        assertEquals(Reminder(preset("In 30 minutes").startAt, 30, ReminderKind.Alarm), entries().single().reminder)
        onNodeWithText("Alarm ${ReminderRules.clock(preset("In 30 minutes").startAt, zone)}, every 30 min").assertExists()
    }

    @Test
    fun `the current kind is selected when the sheet opens and can be changed keeping the time`() = runComposeUiTest {
        val e = entry()
        val start = preset("In 1 hour").startAt
        runBlocking { store.setReminder(e.id, Reminder(start, 60, ReminderKind.Alarm)) }
        show(alarms = true)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("Alarm").assertIsSelected()
        onNodeWithText("Notification").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        assertEquals(Reminder(start, 60, ReminderKind.Notification), entries().single().reminder)
    }

    @Test
    fun `the template form offers the kind only with a reminder time`() = runComposeUiTest {
        show(alarms = true)
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Alarm").assertDoesNotExist()

        onNodeWithText("Set a time").performClick()
        onNodeWithText("OK").performClick()
        reveal("Alarm")
        onNodeWithText("Notification").assertIsSelected()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Alarm").assertIsSelected()
    }

    @Test
    fun `a template saved with an alarm keeps it and the form shows it again`() = runComposeUiTest {
        show(alarms = true)
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Meds")
        onNodeWithText("Set a time").performClick()
        onNodeWithText("OK").performClick()
        reveal("Alarm")
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save").performClick()
        waitForIdle()

        val template = runBlocking { store.templates.first().single() }
        assertEquals(ReminderKind.Alarm, template.reminder?.kind)

        onNodeWithContentDescription("Edit Meds").performClick()
        reveal("Alarm")
        onNodeWithText("Alarm").assertIsSelected()
    }

    @Test
    fun `the template form on a platform without alarms offers no kind`() = runComposeUiTest {
        show(alarms = false)
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Set a time").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithText("Alarm").assertDoesNotExist()
    }

    @Test
    fun `a task generated from an alarm template has an alarm and can be changed alone`() = runComposeUiTest {
        runBlocking {
            store.create("Meds", Tier.One, Energy.Low, schedule = TaskSchedule.Daily, reminder = TemplateReminder(540, 30, ReminderKind.Alarm))
            store.generateDue(today, zone)
        }
        show(alarms = true)
        onNodeWithContentDescription("Options for Meds").performClick()
        onNodeWithText("Alarm").assertIsSelected()
        onNodeWithText("Notification").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        assertEquals(ReminderKind.Notification, entries().single().reminder?.kind)
        assertEquals(ReminderKind.Alarm, runBlocking { store.templates.first().single().reminder?.kind })
        assertNotNull(entries().single().reminder)
    }

    private val fullScreenMessage = "Full-screen alarms are turned off for OpenAdhd. The alarm will still ring, but it will not appear over the lock screen."

    @Test
    fun `saving an alarm without full screen alerts says so and the button opens the setting`() = runComposeUiTest {
        entry()
        val scheduler = FakeReminderScheduler(supportsAlarms = true, fullScreenAllowed = false)
        show(alarms = true, scheduler = scheduler)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()

        assertEquals(ReminderKind.Alarm, entries().single().reminder?.kind)
        onNodeWithText(fullScreenMessage).assertExists()
        onNodeWithText("Open settings").performClick()
        assertEquals(1, scheduler.settingsOpened)
    }

    @Test
    fun `no message for a notification reminder even when full screen alerts are off`() = runComposeUiTest {
        entry()
        show(alarms = true, fullScreenAllowed = false)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        onNodeWithText(fullScreenMessage).assertDoesNotExist()
        onNodeWithText("Open settings").assertDoesNotExist()
    }

    @Test
    fun `no message when full screen alerts are allowed`() = runComposeUiTest {
        entry()
        show(alarms = true, fullScreenAllowed = true)
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        onNodeWithText(fullScreenMessage).assertDoesNotExist()
    }

    @Test
    fun `a template alarm without full screen alerts shows the notice once after saving`() = runComposeUiTest {
        show(alarms = true, fullScreenAllowed = false)
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Meds")
        onNodeWithText("Set a time").performClick()
        onNodeWithText("OK").performClick()
        reveal("Alarm")
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save").performClick()
        waitForIdle()

        onNodeWithText("Meds").assertExists()
        onNodeWithText(com.usuallycoding.openadhd.ui.templates.TemplatesViewModel.FullScreenOff).assertExists()
    }

    private val alarmsOffMessage = "Alarms are turned off for OpenAdhd, so this reminder will be shown as a notification. Turn them on in your device's settings."

    @Test
    fun `saving an alarm when alarms are refused says so and keeps the alarm`() = runComposeUiTest {
        entry()
        show(alarms = true, scheduler = FakeReminderScheduler(supportsAlarms = true, alarmsAllowed = false))
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Alarm").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        assertEquals(ReminderKind.Alarm, entries().single().reminder?.kind)
        onNodeWithText(alarmsOffMessage).assertExists()
    }

    @Test
    fun `no alarms message for a notification reminder or when alarms are allowed`() = runComposeUiTest {
        entry()
        show(alarms = true, scheduler = FakeReminderScheduler(supportsAlarms = true, alarmsAllowed = false))
        onNodeWithContentDescription("Options for Clean the kitchen").performClick()
        onNodeWithText("In 30 minutes").performClick()
        onNodeWithText("Save reminder").performClick()
        waitForIdle()
        onNodeWithText(alarmsOffMessage).assertDoesNotExist()
    }
}
