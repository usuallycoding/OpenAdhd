package com.usuallycoding.openadhd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** The app tells the reminder scheduler when it comes to the front, so a platform can arm its reminders again. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AppResumeUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the scheduler is told when the app is shown`() = runComposeUiTest {
        val scheduler = FakeReminderScheduler()
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null, reminders = scheduler)) }
        waitForIdle()

        assertTrue(scheduler.resumes > 0)
    }

    @Test
    fun `an app with no reminder scheduler still starts`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        waitForIdle()

        onNodeWithContentDescription("Dopamine coins").assertExists()
    }
}
