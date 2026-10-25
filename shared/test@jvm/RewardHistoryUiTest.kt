package com.usuallycoding.openadhd

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Instant

/** The Shop's History tab, with the clock fixed to Saturday 24 October 2026 around noon. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class RewardHistoryUiTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val clock = FakeClock(now)

    /** Redemptions on the 20th and 23rd, and two today; one of today's has no description. */
    private fun historyStore(): InMemoryStore {
        val store = InMemoryStore(clock)
        runBlocking {
            fun redeemAt(at: String, name: String, description: String) {
                clock.current = Instant.parse(at)
                runBlocking {
                    val reward = store.createReward(name, RewardTier.One, Energy.Low, description)
                    store.redeem(reward.id, name)
                }
            }
            redeemAt("2026-10-20T10:00:00Z", "Old tea", "Cold one")
            redeemAt("2026-10-23T10:00:00Z", "Yesterday snack", "Crunchy")
            redeemAt("2026-10-24T09:00:00Z", "Movie night", "Pick anything, no guilt")
            redeemAt("2026-10-24T11:00:00Z", "Plain tea", "")
        }
        clock.current = now
        return store
    }

    private fun ComposeUiTest.openShopHistory(store: InMemoryStore) {
        setContent { App(AppContainer(store, clock, dateCheckInterval = null)) }
        onNodeWithText("Shop").performClick()
        onNodeWithText("History").performClick()
    }

    /** A day in the date picker. The picker marks the real current day with a "Today, " prefix, which would make tests depend on when they run. */
    private fun ComposeUiTest.calendarDay(label: String) = onNode(hasText(label, substring = true))

    @Test
    fun `the history opens on today with only today's redemptions their descriptions and prices`() = runComposeUiTest {
        openShopHistory(historyStore())
        onNodeWithText("Sat 24 Oct 2026").assertExists()
        onNodeWithText("Movie night").assertExists()
        onNodeWithText("Pick anything, no guilt").assertExists()
        onNodeWithText("Plain tea").assertExists()
        onNodeWithText("Yesterday snack").assertDoesNotExist()
        onNodeWithText("Old tea").assertDoesNotExist()
        onNodeWithContentDescription("Next day").assertDoesNotExist()
    }

    @Test
    fun `stepping back shows another day and next returns to today`() = runComposeUiTest {
        openShopHistory(historyStore())
        onNodeWithContentDescription("Previous day").performClick()
        onNodeWithText("Fri 23 Oct 2026").assertExists()
        onNodeWithText("Yesterday snack").assertExists()
        onNodeWithText("Crunchy").assertExists()
        onNodeWithText("Movie night").assertDoesNotExist()

        onNodeWithContentDescription("Next day").performClick()
        onNodeWithText("Sat 24 Oct 2026").assertExists()
        onNodeWithText("Movie night").assertExists()
        onNodeWithContentDescription("Next day").assertDoesNotExist()
    }

    @Test
    fun `a day with no redemptions says so and offers no controls`() = runComposeUiTest {
        openShopHistory(InMemoryStore(clock))
        onNodeWithText("Sat 24 Oct 2026").assertExists()
        onNodeWithText("Nothing redeemed on this day").assertExists()
        onNodeWithContentDescription("Redeem Anything").assertDoesNotExist()
    }

    @Test
    fun `the calendar allows only days up to today that have redemptions and OK picks one`() = runComposeUiTest {
        openShopHistory(historyStore())
        onNodeWithText("Sat 24 Oct 2026").performClick()

        calendarDay("Tuesday, October 20, 2026").assertIsEnabled()
        calendarDay("Friday, October 23, 2026").assertIsEnabled()
        calendarDay("Saturday, October 24, 2026").assertIsEnabled()
        calendarDay("Wednesday, October 21, 2026").assertIsNotEnabled()
        calendarDay("Thursday, October 22, 2026").assertIsNotEnabled()
        calendarDay("Sunday, October 25, 2026").assertIsNotEnabled()

        calendarDay("Tuesday, October 20, 2026").performClick()
        onNodeWithText("OK").performClick()
        onNodeWithText("Tue 20 Oct 2026").assertExists()
        onNodeWithText("Old tea").assertExists()
        onNodeWithText("Cold one").assertExists()
        onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun `cancelling the calendar keeps the day`() = runComposeUiTest {
        openShopHistory(historyStore())
        onNodeWithText("Sat 24 Oct 2026").performClick()
        calendarDay("Tuesday, October 20, 2026").performClick()
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Sat 24 Oct 2026").assertExists()
        onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun `the chosen day is kept after visiting another screen`() = runComposeUiTest {
        openShopHistory(historyStore())
        onNodeWithContentDescription("Previous day").performClick()
        onNodeWithText("Today").performClick()
        onNodeWithText("Shop").performClick()
        onNodeWithText("Fri 23 Oct 2026").assertExists()
        onNodeWithText("Yesterday snack").assertExists()
    }
}
