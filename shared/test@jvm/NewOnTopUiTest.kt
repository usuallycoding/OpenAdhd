package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** A newly created task template or reward goes to the top of every list that shows it. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class NewOnTopUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun ComposeUiTest.assertOrder(vararg texts: String) {
        val tops = texts.map { onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops, "expected ${texts.toList()} top to bottom")
    }

    @Test
    fun `a task template created in the form is first in the Tasks tab and in the picker`() = runComposeUiTest {
        val store = InMemoryStore()
        // New templates go to the top, so they are created last to first to list Alpha, Beta.
        runBlocking {
            store.create("Beta", Tier.One, Energy.Low)
            store.create("Alpha", Tier.One, Energy.Low)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
        assertOrder("Alpha", "Beta")

        onNodeWithContentDescription("Add template").performClick()
        onNodeWithText("Name").performTextInput("Newest")
        onNodeWithText("Save").performClick()
        assertOrder("Newest", "Alpha", "Beta")

        onNodeWithText("Today").performClick()
        onNodeWithContentDescription("Add task to today").performClick()
        assertOrder("Newest", "Alpha", "Beta")
    }

    @Test
    fun `a reward created in the form is first in the Rewards tab and on its shelf in the Shop`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking { store.createReward("Tea", RewardTier.One, Energy.Low) }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
        onNodeWithText("Rewards").performClick()

        onNodeWithContentDescription("Add reward").performClick()
        onNodeWithText("Name").performTextInput("Cake")
        onNodeWithText("Save").performClick()
        assertOrder("Cake", "Tea")

        onNodeWithText("Shop").performClick()
        assertOrder("Cake", "Tea")
    }

    @Test
    fun `tier five rewards stay ordered by price in the Shop whatever the list order is`() = runComposeUiTest {
        val store = InMemoryStore()
        runBlocking {
            store.createReward("Cheap", RewardTier.Five, Energy.Low, customPrice = 301)
            store.createReward("Pricey", RewardTier.Five, Energy.Low, customPrice = 500)
        }
        setContent { App(AppContainer(store, dateCheckInterval = null)) }
        onNodeWithText("Templates").performClick()
        onNodeWithText("Rewards").performClick()
        assertOrder("Pricey", "Cheap")

        onNodeWithText("Shop").performClick()
        assertOrder("Cheap", "Pricey")
    }
}
