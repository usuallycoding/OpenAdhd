package com.usuallycoding.openadhd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AppUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `create a template plan it complete it and see coins`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        // starts on Today, empty, balance 0
        onNodeWithText("Nothing planned for today.").assertExists()
        onNodeWithContentDescription("Dopamine coins").assertExists()

        // Templates: blank name is rejected, then a real template is created
        onNodeWithText("Templates").performClick()
        onNodeWithText("No templates yet. Tap + to create your first task.").assertExists()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Save").performClick()
        onNodeWithText("A name is required").assertExists()
        onNode(hasSetTextAction()).performTextInput("Reply to emails")
        onNodeWithText("2").performClick()
        onNodeWithText("Save").performClick()
        onNodeWithText("Reply to emails").assertExists()
        onNodeWithText("Tier 2 · Medium energy").assertExists()

        // Today: add it, complete it, balance becomes 10
        onNodeWithText("Today").performClick()
        onNodeWithText("Add a task").performClick()
        onNodeWithText("Reply to emails").performClick()
        onNodeWithText("Tier 2 · Medium energy · +10 coins").assertExists()
        onNode(isToggleable()).performClick()
        onNodeWithText("10").assertExists()
    }

    @Test
    fun `theme chooser switches the selected mode`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithContentDescription("Theme: Follow system theme").performClick()
        onNodeWithText("Dark").performClick()
        onNodeWithContentDescription("Theme: Dark").assertExists()
    }
}
