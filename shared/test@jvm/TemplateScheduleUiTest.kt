package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The schedule and reminder controls of the template form, and the schedule line in the list. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class TemplateScheduleUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun ComposeUiTest.openTemplates(store: InMemoryStore) {
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
    }

    private fun ComposeUiTest.tap(text: String) = onNodeWithText(text).performClick()

    @Test
    fun `a weekly template is saved with the chosen days and listed with them`() = runComposeUiTest {
        val store = InMemoryStore()
        openTemplates(store)
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Laundry")
        tap("Weekly")
        // The schedule is at the bottom of a tall form, so scroll it into view before choosing the days.
        onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText("Wed"))
        tap("Mon")
        tap("Wed")
        tap("Fri")
        tap("Save")

        onNodeWithText("Mon, Wed, Fri").assertExists()
        assertEquals(
            TaskSchedule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
            runBlocking { store.templates.first().single().schedule },
        )
    }

    @Test
    fun `a weekly template without a day is refused with a message`() = runComposeUiTest {
        val store = InMemoryStore()
        openTemplates(store)
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Laundry")
        tap("Weekly")
        tap("Save")

        onNodeWithText("Choose at least one day of the week").assertExists()
        assertEquals(0, runBlocking { store.templates.first().size })
    }

    @Test
    fun `a period starts out valid and is saved (a backwards one is refused in the view model)`() = runComposeUiTest {
        val store = InMemoryStore()
        openTemplates(store)
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Trip")
        tap("Period")
        tap("Save")
        assertEquals(1, runBlocking { store.templates.first().size })
    }

    @Test
    fun `the repeat choices appear only once a reminder time is set`() = runComposeUiTest {
        openTemplates(InMemoryStore())
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Set a time").assertExists()
        onNodeWithText("Every 15 min").assertDoesNotExist()

        tap("Set a time")
        tap("OK")
        onNodeWithText("Every 15 min").assertExists()
        onNodeWithText("Clear time").assertExists()

        tap("Clear time")
        onNodeWithText("Every 15 min").assertDoesNotExist()
    }

    @Test
    fun `the list shows daily and ended schedules and none for manual templates`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking {
            store.create("Manual", Tier.One, Energy.Low)
            store.create("Everyday", Tier.One, Energy.Low, schedule = TaskSchedule.Daily)
            store.create("Past", Tier.One, Energy.Low, schedule = TaskSchedule.OnDate(LocalDate(2020, 1, 1)))
            store.create("Soon", Tier.One, Energy.Low, schedule = TaskSchedule.OnDate(LocalDate(2999, 1, 2)))
        }
        openTemplates(store)
        onNodeWithText("Every day").assertExists()
        onNodeWithText("Ended").assertExists()
        onNodeWithText("On 2 Jan 2999").assertExists()
        onNodeWithText("Manual").assertExists()
    }

    @Test
    fun `saving a template reminder with notifications refused closes the form and shows the notice`() = runComposeUiTest {
        val store = InMemoryStore()
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = false)
        setContent { App(AppContainer(store, dateCheckInterval = null, reminders = scheduler)) }
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Meds")
        tap("Set a time")
        tap("OK")
        tap("Save")

        onNodeWithText("Meds").assertExists()
        onNodeWithText("Notifications are turned off for OpenAdhd. Turn them on in your device's settings to get reminders.").assertExists()
        assertEquals(1, scheduler.permissionRequests)
        assertNotNull(runBlocking { store.templates.first().single().reminder })
    }

    @Test
    fun `saving a template reminder with notifications allowed shows no notice`() = runComposeUiTest {
        val store = InMemoryStore()
        val scheduler = FakeReminderScheduler(granted = false, grantsWhenAsked = true)
        setContent { App(AppContainer(store, dateCheckInterval = null, reminders = scheduler)) }
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Meds")
        tap("Set a time")
        tap("OK")
        tap("Save")

        onNodeWithText("Meds").assertExists()
        onNodeWithText("Notifications are turned off for OpenAdhd. Turn them on in your device's settings to get reminders.").assertDoesNotExist()
    }
}
