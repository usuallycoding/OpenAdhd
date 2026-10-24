package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsSelected
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Instant
import kotlin.test.assertEquals

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
        // the finished entry folds away and reports when it was done
        onNodeWithText("Tier 3 · Medium energy · 3 of 3 done · Done ", substring = true).assertExists()
        onAllNodes(isToggleable()).assertCountEquals(0)
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
    fun `cancelling a form sheet discards what was typed`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Never saved")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("Never saved").assertDoesNotExist()
        onNodeWithText("No templates yet. Tap + to create your first task.").assertExists()

        onNodeWithText("Rewards").performClick()
        onNodeWithContentDescription("Add reward").performClick()
        onNodeWithText("Name").performTextInput("Also never saved")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("Also never saved").assertDoesNotExist()
        onNodeWithText("No rewards yet. Tap + to add something to look forward to.").assertExists()
    }

    @Test
    fun `a tall task form with ten subtasks still lets you reach Save`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Big job")
        onNodeWithText("4").performClick()
        repeat(10) { onNodeWithText("Add subtask").performScrollTo().performClick() }
        onNodeWithText("Subtasks (10/10)").assertExists()
        onNodeWithText("Add subtask").performScrollTo().assertIsNotEnabled()
        for (i in 1..10) onNodeWithText("Subtask $i").performScrollTo().performTextInput("Step $i")
        onNodeWithText("Save").assertExists().performClick()
        waitForIdle()
        onNodeWithText("Big job").assertExists()
        onNodeWithText("Tier 4 · Medium energy · 10 subtasks").assertExists()
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
        // 40 coins do not cover the 50-coin reward, but it can be bought on credit, so nothing is blocked
        onAllNodesWithText("more coins needed", substring = true).assertCountEquals(0)
        onNodeWithContentDescription("Redeem Movie night").assertIsEnabled()
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

    // ---- mystery rolls

    private fun rollingStore(random: kotlin.random.Random, vararg extraTasks: Tier): InMemoryStore {
        val store = InMemoryStore(random = random)
        runBlocking {
            store.createReward("Movie night", RewardTier.Two, Energy.Low)
            store.createReward("Dinner out", RewardTier.Three, Energy.High)
            for (tier in extraTasks) {
                val entry = store.assign(store.create("Job ${tier.level}", tier, Energy.High), LocalDate(2026, 10, 23))
                store.completeTask(entry.id)
            }
        }
        return store
    }

    @Test
    fun `a lucky upgrade roll spends the tier price and is revealed and recorded`() = runComposeUiTest {
        val store = rollingStore(ScriptedRandom(0, 0), Tier.Four, Tier.Three)
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("60").assertExists()

        onNodeWithText("Shop").performClick()
        onNodeWithText("Mystery roll").assertExists()
        onNodeWithText("Tier 2 roll · 50 coins").assertExists()
        onNodeWithText("A random tier 2 reward, with a 1 in 6 chance of a tier 3 one").assertExists()

        // cancelling spends nothing
        onNodeWithContentDescription("Roll tier 2").performClick()
        onNodeWithText("Roll tier 2?").assertExists()
        onNodeWithText("Cancel").performClick()
        onNodeWithText("60").assertExists()

        // confirming rolls: the upgrade is revealed and only the tier 2 price is spent
        onNodeWithContentDescription("Roll tier 2").performClick()
        onNodeWithText("Confirm").performClick()
        onNodeWithText("You got: Dinner out!").assertExists()
        onNodeWithText("Lucky upgrade! You rolled tier 2 and won a tier 3 reward.").assertExists()
        onNodeWithText("Close").performClick()
        onNodeWithText("10").assertExists()

        onNodeWithText("History").performClick()
        onNodeWithText("Dinner out").assertExists()
        onNodeWithText("-50").assertExists()
        onNodeWithText("Mystery roll of tier 2 · Lucky upgrade (tier 3 reward)", substring = true).assertExists()
    }

    @Test
    fun `a usual roll reveals the reward without an upgrade message`() = runComposeUiTest {
        val store = rollingStore(ScriptedRandom(3, 0), Tier.Four, Tier.Three)
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onNodeWithText("Shop").performClick()
        onNodeWithContentDescription("Roll tier 2").performClick()
        onNodeWithText("Confirm").performClick()
        onNodeWithText("You got: Movie night!").assertExists()
        onNodeWithText("Lucky upgrade!", substring = true).assertDoesNotExist()
        onNodeWithText("Close").performClick()

        onNodeWithText("History").performClick()
        onNodeWithText("Mystery roll of tier 2", substring = true).assertExists()
        onNodeWithText("Lucky upgrade", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a roll of an empty tier says why and is disabled while one that can borrow is enabled`() = runComposeUiTest {
        val store = rollingStore(ScriptedRandom(), Tier.Three, Tier.Two)
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onNodeWithText("Shop").performClick()
        onNodeWithText("No tier 1 rewards yet").assertExists()
        onNodeWithContentDescription("Roll tier 1").assertIsNotEnabled()
        // 30 coins are not enough for the 50-coin tier 2 roll, but the debt limit lets the user borrow the rest
        onAllNodesWithText("more coins needed", substring = true).assertCountEquals(0)
        onNodeWithContentDescription("Roll tier 2").assertIsEnabled()
    }

    @Test
    fun `earlier purchases stay plain purchases next to rolls in the history`() = runComposeUiTest {
        val store = rollingStore(ScriptedRandom(3, 0), Tier.Four, Tier.Four, Tier.Four)
        runBlocking {
            val reward = store.rewards.first().first { it.name == "Movie night" }
            store.redeem(reward.id, "buy1")
            store.mysteryRoll(RewardTier.Two, "roll1")
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onNodeWithText("Shop").performClick()
        onNodeWithText("History").performClick()
        onAllNodesWithText("Mystery roll of tier", substring = true).assertCountEquals(1)
        onAllNodesWithText("Tier 2 · ", substring = true).assertCountEquals(1)
    }

    // ---- critical hits

    private fun critStore(vararg answers: Boolean, subtasks: List<String> = emptyList(), tier: Tier = Tier.Three): InMemoryStore {
        val store = InMemoryStore(criticalHits = ScriptedCrits(*answers))
        runBlocking {
            val template = store.create(if (subtasks.isEmpty()) "Taxes" else "Clean", tier, Energy.Low, subtasks = subtasks)
            store.assign(template, kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault()))
        }
        return store
    }

    @Test
    fun `a critical subtask shows a message and pays double`() = runComposeUiTest {
        val store = critStore(true, false, subtasks = listOf("A", "B", "C"))
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Critical", substring = true).assertDoesNotExist()
        onAllNodesWithText("+7").assertCountEquals(2)

        onAllNodes(isToggleable())[0].performClick()
        onNodeWithText("Critical hit! +7 bonus coins").assertExists()
        onNodeWithText("14").assertExists()

        // the message does not get in the way of completing the next item, which is not critical
        onAllNodes(isToggleable())[1].performClick()
        onNodeWithText("21").assertExists()
    }

    @Test
    fun `a critical task without subtasks pays its reward twice`() = runComposeUiTest {
        val store = critStore(true)
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Tier 3 · Low energy · +20 coins", substring = true).assertExists()

        onNode(isToggleable()).performClick()
        onNodeWithText("Critical hit! +20 bonus coins").assertExists()
        onNodeWithText("40").assertExists()
    }

    @Test
    fun `a usual completion shows no message`() = runComposeUiTest {
        val store = critStore(false, subtasks = listOf("A", "B", "C"))
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onAllNodes(isToggleable())[0].performClick()

        onNodeWithText("7").assertExists()
        onNodeWithText("Critical", substring = true).assertDoesNotExist()
    }

    // ---- smart randomizer

    private fun randomizerStore(vararg templates: Pair<String, Energy>, withKitchen: Boolean = false): InMemoryStore {
        val store = InMemoryStore()
        runBlocking {
            templates.forEach { (name, energy) -> store.create(name, Tier.Two, energy) }
            if (withKitchen) {
                store.create("Clean the kitchen", Tier.Three, Energy.High, "Whole room", listOf("Dishes", "Counters", "Trash"))
            }
        }
        return store
    }

    private fun randomizerContent(store: InMemoryStore, vararg draws: Int): AppContainer =
        AppContainer(store, dateCheckInterval = null, random = ScriptedRandom(*draws))

    @Test
    fun `pick one for me suggests a task and accepting adds it to today`() = runComposeUiTest {
        // High day over [Dishes (Low, 1), Emails (Medium, 2), Clean the kitchen (High, 4)]: draw 3 lands on the kitchen
        val store = randomizerStore("Dishes" to Energy.Low, "Emails" to Energy.Medium, withKitchen = true)
        setContent { App(randomizerContent(store, 3)) }

        onNodeWithText("Pick one for me").performClick()
        onNodeWithText("How much energy do you have?").assertExists()
        onNodeWithText("High").performClick()

        onNodeWithText("How about this one?").assertExists()
        onNodeWithText("Clean the kitchen").assertExists()
        onNodeWithText("Whole room").assertExists()
        onNodeWithText("Tier 3 · High energy · 3 subtasks").assertExists()
        onNodeWithText("20 coins").assertExists()
        // nothing is added just by suggesting
        onNodeWithText("Nothing planned for today.").assertExists()

        onNodeWithText("Add to today").performClick()

        onNodeWithText("How about this one?").assertDoesNotExist()
        onNodeWithText("Clean the kitchen").assertExists()
        onNodeWithText("Tier 3 · High energy · 0 of 3 done").assertExists()
        onAllNodesWithText("+7").assertCountEquals(2)
    }

    @Test
    fun `another shows different tasks and then says that was everything`() = runComposeUiTest {
        // Medium day over [Dishes (Low, 2), Emails (Medium, 4)]: draw 0 is Dishes, then only Emails is left
        val store = randomizerStore("Dishes" to Energy.Low, "Emails" to Energy.Medium, withKitchen = true)
        setContent { App(randomizerContent(store, 0, 0)) }

        // the dice button is always there, also when Today is empty
        onNodeWithContentDescription("Pick one for me").assertExists()
        onNodeWithText("Pick one for me").performClick()
        onNodeWithText("Medium").performClick()
        onNodeWithText("Dishes").assertExists()

        onNodeWithText("Another").performClick()
        onNodeWithText("Emails").assertExists()
        onNodeWithText("Clean the kitchen").assertDoesNotExist() // needs more energy than Medium

        onNodeWithText("Another").performClick()
        onNodeWithText("That was everything that fits").assertExists()
        onNodeWithText("Choose another energy").performClick()
        onNodeWithText("How much energy do you have?").assertExists()
    }

    @Test
    fun `not now leaves today unchanged`() = runComposeUiTest {
        val store = randomizerStore("Dishes" to Energy.Low)
        setContent { App(randomizerContent(store, 0)) }

        onNodeWithText("Pick one for me").performClick()
        onNodeWithText("Low").performClick()
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Not now").performClick()

        onNodeWithText("How about this one?").assertDoesNotExist()
        onNodeWithText("Nothing planned for today.").assertExists()
    }

    @Test
    fun `low energy never offers a task needing more and says when nothing fits`() = runComposeUiTest {
        val store = randomizerStore("Emails" to Energy.Medium, withKitchen = true)
        setContent { App(randomizerContent(store)) }

        onNodeWithText("Pick one for me").performClick()
        onNodeWithText("Low").performClick()

        onNodeWithText("Nothing fits Low energy").assertExists()
        onNodeWithText("How about this one?").assertDoesNotExist()
    }

    @Test
    fun `a task already on today's list is never suggested`() = runComposeUiTest {
        val store = randomizerStore("Dishes" to Energy.Medium, "Emails" to Energy.Medium)
        runBlocking {
            val planned = store.templates.first().first { it.name == "Emails" }
            store.assign(planned, kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault()))
        }
        setContent { App(randomizerContent(store, 0)) }

        onNodeWithContentDescription("Pick one for me").performClick()
        onNodeWithText("Medium").performClick()

        onNodeWithText("How about this one?").assertExists()
        onAllNodesWithText("Dishes").assertCountEquals(1) // only the suggestion; Emails is on the list already
    }

    @Test
    fun `with no task templates it tells the user to create one`() = runComposeUiTest {
        setContent { App(randomizerContent(InMemoryStore())) }

        onNodeWithText("Pick one for me").performClick()

        onNodeWithText("You have no task templates yet. Create one on the Templates tab first.").assertExists()
        onNodeWithText("How about this one?").assertDoesNotExist()
    }

    // ---- collapse and expand

    @Test
    fun `tapping an entry hides and shows its subtasks and keeps its progress visible`() = runComposeUiTest {
        val store = critStore(subtasks = listOf("Dishes", "Counters", "Trash"))
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Tier 3 · Low energy · 0 of 3 done").assertExists()

        onNodeWithText("Clean").performClick()
        onNodeWithText("Dishes").assertDoesNotExist()
        onNodeWithText("Trash").assertDoesNotExist()
        onNodeWithText("Clean").assertExists()
        onNodeWithText("Tier 3 · Low energy · 0 of 3 done").assertExists()

        onNodeWithText("Clean").performClick()
        onNodeWithText("Dishes").assertExists()
        onNodeWithText("Trash").assertExists()
    }

    @Test
    fun `an entry without subtasks ignores the tap and still completes with its checkbox`() = runComposeUiTest {
        val store = critStore(false)
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onNodeWithText("Taxes").performClick()
        onNodeWithText("Tier 3 · Low energy · +20 coins", substring = true).assertExists()

        onNode(isToggleable()).performClick()
        onNodeWithText("20").assertExists()
    }

    @Test
    fun `a collapsed entry stays collapsed after visiting another screen`() = runComposeUiTest {
        val store = critStore(subtasks = listOf("Dishes", "Counters", "Trash"))
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onNodeWithText("Clean").performClick()
        onNodeWithText("Dishes").assertDoesNotExist()

        onNodeWithText("Shop").performClick()
        onNodeWithText("Today").performClick()

        onNodeWithText("Clean").assertExists()
        onNodeWithText("Dishes").assertDoesNotExist()
    }

    // ---- swipeable and remembered tabs

    private fun androidx.compose.ui.test.ComposeUiTest.swipeContentLeft() = onRoot().performTouchInput { swipeLeft() }

    private fun androidx.compose.ui.test.ComposeUiTest.swipeContentRight() = onRoot().performTouchInput { swipeRight() }

    @Test
    fun `swiping changes the tab on Templates and the tab row follows`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
        onNodeWithText("Tasks").assertIsSelected()
        onNodeWithText("No templates yet. Tap + to create your first task.").assertExists()

        swipeContentLeft()
        onNodeWithText("Rewards").assertIsSelected()
        onNodeWithText("No rewards yet. Tap + to add something to look forward to.").assertExists()

        swipeContentRight()
        onNodeWithText("Tasks").assertIsSelected()
        onNodeWithText("No templates yet. Tap + to create your first task.").assertExists()
    }

    @Test
    fun `swiping past the first or last tab changes nothing`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()

        swipeContentRight()
        onNodeWithText("Tasks").assertIsSelected()

        swipeContentLeft()
        swipeContentLeft()
        onNodeWithText("Rewards").assertIsSelected()
    }

    @Test
    fun `swiping changes the tab on the Shop`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()
        onNodeWithText("Rewards").assertIsSelected()

        swipeContentLeft()
        onNodeWithText("History").assertIsSelected()
        onNodeWithText("Nothing redeemed yet.").assertExists()

        swipeContentRight()
        onNodeWithText("Rewards").assertIsSelected()
    }

    @Test
    fun `tapping a tab shows it`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()

        onNodeWithText("History").performClick()

        onNodeWithText("History").assertIsSelected()
        onNodeWithText("Nothing redeemed yet.").assertExists()
    }

    @Test
    fun `each screen remembers its own tab after visiting another screen`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithText("Rewards").performClick()
        onNodeWithText("Shop").performClick()
        onNodeWithText("History").performClick()
        onNodeWithText("Today").performClick()

        onNodeWithText("Templates").performClick()
        onNodeWithText("Rewards").assertIsSelected()
        onNodeWithText("Shop").performClick()
        onNodeWithText("History").assertIsSelected()
    }

    @Test
    fun `a screen that was never changed starts on its first tab`() = runComposeUiTest {
        setContent { App(AppContainer(InMemoryStore(), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()
        onNodeWithText("History").performClick()

        onNodeWithText("Templates").performClick()

        onNodeWithText("Tasks").assertIsSelected()
    }

    // ---- reordering

    private fun ComposeUiTest.topOf(text: String) = onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    private fun ComposeUiTest.assertOrder(vararg texts: String) {
        val tops = texts.map { topOf(it) }
        assertEquals(tops.sorted(), tops, "expected ${texts.toList()} top to bottom")
    }

    private fun reorderStore(templates: List<String> = listOf("Alpha", "Beta", "Gamma"), planToday: Boolean = false): InMemoryStore {
        val store = InMemoryStore()
        runBlocking {
            val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
            templates.forEach {
                val template = store.create(it, Tier.One, Energy.Low)
                if (planToday) store.assign(template, today)
            }
        }
        return store
    }

    @Test
    fun `reorder mode on task templates shows arrows hides editing and moves a template`() = runComposeUiTest {
        setContent { App(AppContainer(reorderStore(), dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
        assertOrder("Alpha", "Beta", "Gamma")
        onNodeWithContentDescription("Edit Alpha").assertExists()

        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Edit Alpha").assertDoesNotExist()
        onNodeWithContentDescription("Delete Alpha").assertDoesNotExist()
        onNodeWithContentDescription("Add template").assertDoesNotExist()
        onNodeWithContentDescription("Move Alpha up").assertIsNotEnabled()
        onNodeWithContentDescription("Move Alpha down").assertIsEnabled()
        onNodeWithContentDescription("Move Gamma down").assertIsNotEnabled()

        onNodeWithContentDescription("Move Alpha down").performClick()
        assertOrder("Beta", "Alpha", "Gamma")
        onNodeWithContentDescription("Move Gamma up").performClick()
        assertOrder("Beta", "Gamma", "Alpha")

        onNodeWithContentDescription("Finish reordering").performClick()
        onNodeWithContentDescription("Move Beta down").assertDoesNotExist()
        onNodeWithContentDescription("Edit Beta").assertExists()
        onNodeWithContentDescription("Add template").assertExists()
        assertOrder("Beta", "Gamma", "Alpha")
    }

    @Test
    fun `there is no reorder toggle with fewer than two items`() = runComposeUiTest {
        setContent { App(AppContainer(reorderStore(listOf("Only")), dateCheckInterval = null)) }
        onNodeWithContentDescription("Reorder").assertDoesNotExist()
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Reorder").assertDoesNotExist()
        onNodeWithText("Rewards").performClick()
        onNodeWithContentDescription("Reorder").assertDoesNotExist()
    }

    @Test
    fun `reorder mode on reward templates moves a reward and each tab has its own mode`() = runComposeUiTest {
        val store = reorderStore()
        runBlocking {
            store.createReward("Tea", RewardTier.One, Energy.Low)
            store.createReward("Cake", RewardTier.One, Energy.Low)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Move Alpha down").assertExists()

        // swiping to the Rewards tab leaves the other list's mode, so nothing is left switched on
        swipeContentLeft()
        onNodeWithText("Tea").assertExists()
        onNodeWithContentDescription("Move Tea down").assertDoesNotExist()
        onNodeWithContentDescription("Edit Tea").assertExists()

        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Add reward").assertDoesNotExist()
        onNodeWithContentDescription("Move Tea down").performClick()
        assertOrder("Cake", "Tea")
    }

    @Test
    fun `reorder mode on today moves pending entries and leaves completed ones below without controls`() = runComposeUiTest {
        val store = reorderStore(planToday = true)
        // Alpha is completed before the screen opens: it is shown below the pending entries
        runBlocking {
            val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
            store.completeTask(store.entriesFor(today).first().first { it.name == "Alpha" }.id)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        assertOrder("Beta", "Gamma", "Alpha")

        onNodeWithContentDescription("Reorder").performClick()
        // the only toggleable left is the Finish reordering button itself: no completion checkboxes
        onAllNodes(isToggleable()).assertCountEquals(1)
        onNodeWithContentDescription("Remove Beta from today").assertDoesNotExist()
        onNodeWithContentDescription("Add task to today").assertDoesNotExist()
        onNodeWithContentDescription("Move Alpha down").assertDoesNotExist()
        onNodeWithContentDescription("Move Alpha up").assertDoesNotExist()
        onNodeWithContentDescription("Move Beta up").assertIsNotEnabled()
        onNodeWithContentDescription("Move Gamma down").assertIsNotEnabled()

        onNodeWithContentDescription("Move Beta down").performClick()
        assertOrder("Gamma", "Beta", "Alpha")
    }

    @Test
    fun `today hides subtasks while reordering and keeps them collapsed or expanded afterwards`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking {
            val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
            store.assign(store.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("Dishes", "Floor")), today)
            store.assign(store.create("Walk", Tier.One, Energy.Low), today)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Dishes").assertExists()
        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithText("Dishes").assertDoesNotExist()
        onNodeWithContentDescription("Finish reordering").performClick()
        onNodeWithText("Dishes").assertExists()
    }

    @Test
    fun `the picker and the shop follow the new order`() = runComposeUiTest {
        val store = reorderStore()
        runBlocking {
            store.createReward("Tea", RewardTier.One, Energy.Low)
            store.createReward("Cake", RewardTier.One, Energy.Low)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }

        onNodeWithText("Templates").performClick()
        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Move Gamma up").performClick()
        onNodeWithContentDescription("Move Gamma up").performClick()
        onNodeWithContentDescription("Finish reordering").performClick()
        onNodeWithText("Rewards").performClick()
        onNodeWithContentDescription("Reorder").performClick()
        onNodeWithContentDescription("Move Cake up").performClick()
        onNodeWithContentDescription("Finish reordering").performClick()

        onNodeWithText("Today").performClick()
        onNodeWithContentDescription("Add task to today").performClick()
        assertOrder("Gamma", "Alpha", "Beta")
        onNodeWithText("Close").performClick()

        onNodeWithText("Shop").performClick()
        assertOrder("Cake", "Tea")
    }

    // ---- unchecking and done order

    private fun ComposeUiTest.checkboxes() =
        onAllNodes(isToggleable().and(hasContentDescription("Reorder").not()))

    private fun uncheckStore(clock: FakeClock, vararg names: String, subtasks: List<String> = emptyList(), tier: Tier = Tier.Three): InMemoryStore {
        val store = InMemoryStore(clock)
        runBlocking {
            val today = clock.todayIn(TimeZone.currentSystemDefault())
            names.forEach { store.assign(store.create(it, tier, Energy.Low, subtasks = subtasks), today) }
        }
        return store
    }

    @Test
    fun `unchecking a task takes its coins back and says so`() = runComposeUiTest {
        val clock = FakeClock()
        setContent { App(AppContainer(uncheckStore(clock, "Taxes"), clock, dateCheckInterval = null)) }

        checkboxes()[0].performClick()
        onNodeWithText("20").assertExists()
        onNodeWithText("Tier 3 · Low energy · +20 coins · Done ", substring = true).assertExists()

        checkboxes()[0].performClick()
        onNodeWithText("Undone, -20 coins").assertExists()
        onNodeWithText("0").assertExists()
        onNodeWithText("Tier 3 · Low energy · +20 coins").assertExists()
    }

    @Test
    fun `unchecking a subtask of a finished entry opens it again as in progress`() = runComposeUiTest {
        val clock = FakeClock()
        setContent { App(AppContainer(uncheckStore(clock, "Clean", subtasks = listOf("A", "B", "C")), clock, dateCheckInterval = null)) }

        checkboxes()[0].performClick()
        checkboxes()[1].performClick()
        checkboxes()[2].performClick()
        onNodeWithText("20").assertExists()
        // finished: folded, subtasks hidden
        onNodeWithText("A").assertDoesNotExist()
        onNodeWithText("Clean").performClick()
        onNodeWithText("A").assertExists()

        checkboxes()[2].performClick()
        onNodeWithText("Undone, -6 coins").assertExists()
        onNodeWithText("14").assertExists()
        onNodeWithText("Tier 3 · Low energy · 2 of 3 done").assertExists()
        // pending again, so it is expanded and shows no done time
        onNodeWithText("C").assertExists()
    }

    @Test
    fun `done entries are listed newest first below the pending ones`() = runComposeUiTest {
        val clock = FakeClock()
        val store = uncheckStore(clock, "Alpha", "Beta", "Gamma", tier = Tier.One)
        setContent { App(AppContainer(store, clock, dateCheckInterval = null)) }

        clock.current = Instant.parse("2026-10-23T10:00:00Z")
        checkboxes()[0].performClick()
        // Alpha is done and moved down; Beta is now the first checkbox
        clock.current = Instant.parse("2026-10-23T11:00:00Z")
        checkboxes()[0].performClick()

        assertOrder("Gamma", "Beta", "Alpha")
    }

    @Test
    fun `unchecking spent coins shows a negative balance and the shop still lets the user borrow`() = runComposeUiTest {
        val clock = FakeClock()
        val store = uncheckStore(clock, "Taxes")
        runBlocking { store.createReward("Treat", RewardTier.One, Energy.Low) }
        setContent { App(AppContainer(store, clock, dateCheckInterval = null)) }

        checkboxes()[0].performClick()
        onNodeWithText("Shop").performClick()
        onNodeWithContentDescription("Redeem Treat").performClick()
        onNodeWithText("Confirm").performClick()
        onNodeWithText("Today").performClick()
        checkboxes()[0].performClick()

        onNodeWithText("-20").assertExists()
        onNodeWithText("Shop").performClick()
        onNodeWithContentDescription("Redeem Treat").assertIsEnabled()
    }

    // ---- dopamine loans

    private fun loanStore(random: kotlin.random.Random = kotlin.random.Random.Default): InMemoryStore {
        val store = InMemoryStore(random = random)
        runBlocking {
            store.createReward("Treat", RewardTier.One, Energy.Low)
            store.createReward("Movie night", RewardTier.Two, Energy.Low)
            val earner = store.assign(store.create("Taxes", Tier.Three, Energy.High), LocalDate(2026, 10, 23))
            store.completeTask(earner.id)
        }
        return store
    }

    @Test
    fun `buying on credit names the debt in the confirmation and leaves a negative balance`() = runComposeUiTest {
        setContent { App(AppContainer(loanStore(), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()

        onNodeWithContentDescription("Redeem Movie night").performClick()
        onNodeWithText("Redeem Movie night?").assertExists()
        onNodeWithText("This costs 50 coins. This puts you 30 coins in debt.").assertExists()
        onNodeWithText("Confirm").performClick()

        onNodeWithText("-30").assertExists()
        onNodeWithText("History").performClick()
        onNodeWithText("Movie night").assertExists()
    }

    @Test
    fun `a purchase that stays in credit says nothing about debt`() = runComposeUiTest {
        setContent { App(AppContainer(loanStore(), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()

        onNodeWithContentDescription("Redeem Treat").performClick()
        onNodeWithText("This costs 20 coins.").assertExists()
        onNodeWithText("in debt", substring = true).assertDoesNotExist()
    }

    @Test
    fun `more coins needed appears only past the debt limit`() = runComposeUiTest {
        val store = loanStore()
        runBlocking {
            // 20 earned, then a 480-coin gift: the balance is -460, so the 50-coin reward would end at -510
            val gift = store.createReward("Gift", RewardTier.Five, Energy.Low, customPrice = 480)
            store.redeem(gift.id, "gift")
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()

        onNodeWithText("-460").assertExists()
        onNodeWithContentDescription("Redeem Treat").assertIsEnabled()
        onNodeWithContentDescription("Redeem Movie night").assertIsNotEnabled()
        onNodeWithContentDescription("Roll tier 2").assertIsNotEnabled()
        // the reward and the tier 2 roll both need 10 more
        onAllNodesWithText("10 more coins needed").assertCountEquals(2)
    }

    @Test
    fun `rolling on credit names the debt and spends the price`() = runComposeUiTest {
        setContent { App(AppContainer(loanStore(ScriptedRandom(0, 0)), dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()

        onNodeWithContentDescription("Roll tier 2").performClick()
        onNodeWithText("Roll tier 2?").assertExists()
        onNodeWithText("This puts you 30 coins in debt.", substring = true).assertExists()
        onNodeWithText("Confirm").performClick()
        onNodeWithText("Close").performClick()

        onNodeWithText("-30").assertExists()
    }
}
