package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

/** The version line at the end of Settings. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class VersionLabelUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun ComposeUiTest.openSettings(container: AppContainer) {
        setContent { App(container) }
        onNodeWithText("Settings").performClick()
        onNode(hasScrollAction()).performScrollToNode(hasTestTag("version-label"))
    }

    @Test
    fun `the end of settings shows the version it was given`() = runComposeUiTest {
        openSettings(AppContainer(InMemoryStore(), dateCheckInterval = null, versionLabel = "OpenAdhd 1.0.0 (1)"))
        onNodeWithTag("version-label").assertTextEquals("OpenAdhd 1.0.0 (1)")
    }

    @Test
    fun `the desktop form has no build number`() = runComposeUiTest {
        openSettings(AppContainer(InMemoryStore(), dateCheckInterval = null, versionLabel = "OpenAdhd 1.0.0"))
        onNodeWithTag("version-label").assertTextEquals("OpenAdhd 1.0.0")
    }

    @Test
    fun `a container without a label reads dev`() = runComposeUiTest {
        openSettings(AppContainer(InMemoryStore(), dateCheckInterval = null))
        onNodeWithTag("version-label").assertTextEquals("OpenAdhd dev")
    }

    @Test
    fun `the label is below the delete all data row`() = runComposeUiTest {
        openSettings(AppContainer(InMemoryStore(), dateCheckInterval = null, versionLabel = "OpenAdhd 1.0.0 (1)"))
        val delete = onNodeWithText("Delete all data").getBoundsInRoot().top
        val label = onNodeWithTag("version-label").getBoundsInRoot().top
        assertTrue(delete < label)
    }

    @Test
    fun `tapping the label does nothing`() = runComposeUiTest {
        openSettings(AppContainer(InMemoryStore(), dateCheckInterval = null, versionLabel = "OpenAdhd 1.0.0 (1)"))
        onNodeWithTag("version-label").assertHasNoClickAction()
        onNodeWithTag("version-label").performClick()
        onNodeWithText("Delete all data?").assertDoesNotExist()
        onNodeWithTag("version-label").assertTextEquals("OpenAdhd 1.0.0 (1)")
    }
}
