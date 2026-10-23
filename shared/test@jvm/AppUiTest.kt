package com.usuallycoding.openadhd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
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
        onNodeWithText("Name").performTextInput("Reply to emails")
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

    @Test
    fun `a task with a description and subtasks pays per subtask`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Clean the kitchen")
        onNodeWithText("Description (optional)").performTextInput("Whole room")
        onNodeWithText("3").performClick()
        repeat(3) { onNodeWithText("Add subtask").performScrollTo().performClick() }
        onNodeWithText("Subtask 1").performScrollTo().performTextInput("Dishes")
        onNodeWithText("Subtask 2").performScrollTo().performTextInput("Wipe counters")
        onNodeWithText("Subtask 3").performScrollTo().performTextInput("Take out trash")
        onNodeWithText("Save").performClick()
        onNodeWithText("Tier 3 · Medium energy · 3 subtasks").assertExists()

        onNodeWithText("Today").performClick()
        onNodeWithText("Add a task").performClick()
        onNodeWithText("Clean the kitchen").performClick()

        onNodeWithText("Whole room").assertExists()
        onNodeWithText("Tier 3 · Medium energy · 0 of 3 done").assertExists()
        onAllNodesWithText("+7").assertCountEquals(2)
        onNodeWithText("+6").assertExists()
        // only the three subtasks are checkable; the entry itself has no checkbox
        onAllNodes(isToggleable()).assertCountEquals(3)
        onNodeWithContentDescription("Remove Clean the kitchen from today").assertExists()

        onAllNodes(isToggleable())[0].performClick()
        onNodeWithText("7").assertExists()
        onNodeWithText("Tier 3 · Medium energy · 1 of 3 done").assertExists()
        onNodeWithContentDescription("Remove Clean the kitchen from today").assertDoesNotExist()

        onAllNodes(isToggleable())[1].performClick()
        onAllNodes(isToggleable())[2].performClick()
        onNodeWithText("20").assertExists()
        onNodeWithText("Tier 3 · Medium energy · 3 of 3 done").assertExists()
    }

    @Test
    fun `a blank subtask title is rejected`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Many steps")
        onNodeWithText("Add subtask").performScrollTo().performClick()
        onNodeWithText("Save").performClick()
        onNodeWithText("Every subtask needs a title").assertExists()
    }

    @Test
    fun `the subtask limit follows the chosen tier`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Many steps")

        // tier 1 (the default) allows 5 subtasks
        repeat(5) { onNodeWithText("Add subtask").performScrollTo().performClick() }
        onNodeWithText("Subtasks (5/5)").assertExists()
        onNodeWithText("Add subtask").assertIsNotEnabled()

        // a higher tier raises the limit
        onNodeWithText("2").performScrollTo().performClick()
        onNodeWithText("Subtasks (5/10)").assertExists()
        onNodeWithText("Add subtask").assertIsEnabled()

        // lowering the tier below the count shows an error on save and keeps the dialog open
        repeat(1) { onNodeWithText("Add subtask").performScrollTo().performClick() }
        onNodeWithText("1").performScrollTo().performClick()
        onNodeWithText("Subtask 1").performScrollTo().performTextInput("Step")
        for (i in 2..6) onNodeWithText("Subtask $i").performScrollTo().performTextInput("Step")
        onNodeWithText("Save").performClick()
        onNodeWithText("A tier 1 task can have at most 5 subtasks. Remove some or choose a higher tier.").assertExists()
    }

    @Test
    fun `the overall ceiling of ten applies to high tiers`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Big job")
        onNodeWithText("4").performScrollTo().performClick()

        repeat(10) { onNodeWithText("Add subtask").performScrollTo().performClick() }
        onNodeWithText("Subtasks (10/10)").assertExists()
        onNodeWithText("Add subtask").assertIsNotEnabled()
    }

    @Test
    fun `rewards are priced by tier and tier five needs a price above three hundred`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithText("Rewards").performClick()
        onNodeWithText("No rewards yet. Tap + to add something to look forward to.").assertExists()

        onNodeWithContentDescription("Add reward").performClick()
        onNodeWithText("Name").performTextInput("Movie night")
        onNodeWithText("2").performClick()
        onNodeWithText("Costs 50 coins").assertExists()
        onNodeWithText("Save").performClick()
        onNodeWithText("Tier 2 · Low energy · 50 coins").assertExists()

        onNodeWithContentDescription("Add reward").performClick()
        onNodeWithText("Name").performTextInput("Headphones")
        onNodeWithText("5").performClick()
        onNodeWithText("Save").performClick()
        onNodeWithText("A price is required for tier 5").assertExists()
        onNodeWithText("Price (coins)").performTextInput("300")
        onNodeWithText("Save").performClick()
        onNodeWithText("A tier 5 price must be larger than 300").assertExists()
        onNodeWithText("Price (coins)").performTextReplacement("301")
        onNodeWithText("Save").performClick()
        onNodeWithText("Tier 5 · Low energy · 301 coins").assertExists()
    }

    @Test
    fun `the shop sells affordable rewards after confirming and records the purchase`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking {
            store.createReward("Treat", RewardTier.One, Energy.Low)
            store.createReward("Movie night", RewardTier.Two, Energy.Low)
            val earner = store.assign(store.create("Big job", Tier.Four, Energy.High), LocalDate(2026, 10, 23))
            store.completeTask(earner.id)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("40").assertExists()

        onNodeWithText("Shop").performClick()
        onNodeWithText("Tier 1 · 20 coins").assertExists()
        onNodeWithText("Tier 2 · 50 coins").assertExists()
        onNodeWithText("10 more coins needed").assertExists()
        onNodeWithContentDescription("Redeem Movie night").assertIsNotEnabled()
        onNodeWithContentDescription("Redeem Treat").assertIsEnabled()

        onNodeWithText("History").performClick()
        onNodeWithText("Nothing redeemed yet.").assertExists()
        onNodeWithText("Rewards").performClick()

        // cancelling spends nothing
        onNodeWithContentDescription("Redeem Treat").performClick()
        onNodeWithText("Redeem Treat?").assertExists()
        onNodeWithText("This costs 20 coins.").assertExists()
        onNodeWithText("Cancel").performClick()
        onNodeWithText("40").assertExists()

        // confirming spends the price and records the purchase
        onNodeWithContentDescription("Redeem Treat").performClick()
        onNodeWithText("Confirm").performClick()
        onNodeWithText("20").assertExists()
        onNodeWithText("30 more coins needed").assertExists()

        onNodeWithText("History").performClick()
        onNodeWithText("Treat").assertExists()
        onNodeWithText("-20").assertExists()
    }

    @Test
    fun `the shop explains when there are no rewards`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()
        onNodeWithText("The shop is empty. Add rewards on the Templates screen, in the Rewards tab.").assertExists()
    }
}
