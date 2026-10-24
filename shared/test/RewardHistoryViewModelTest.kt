package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.shop.RewardHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RewardHistoryViewModelTest {
    private val today = MutableStateFlow(LocalDate(2026, 10, 24))
    private val clock = FakeClock(Instant.parse("2026-10-24T12:00:00Z"))
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(timeZone: TimeZone = TimeZone.UTC) = RewardHistoryViewModel(store, today, timeZone)

    /** Earns plenty, then redeems a reward named [name] at [at]. */
    private suspend fun redeem(name: String, at: String, id: String = name) {
        clock.current = Instant.parse(at)
        val template = store.create("Earn", Tier.Four, Energy.Low)
        store.assign(template, LocalDate(2026, 10, 1)).let { store.completeTask(it.id) }
        val reward = store.createReward(name, RewardTier.One, Energy.Low, "About $name")
        store.redeem(reward.id, id)
    }

    @Test
    fun `starts on today and cannot go to a later day`() = runTest {
        val vm = viewModel()
        assertEquals(LocalDate(2026, 10, 24), vm.date.value)
        assertFalse(vm.canGoNext.value)
        vm.nextDay()
        assertEquals(LocalDate(2026, 10, 24), vm.date.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `stepping back and forward ends on today`() = runTest {
        val vm = viewModel()
        vm.previousDay()
        vm.previousDay()
        assertEquals(LocalDate(2026, 10, 22), vm.date.value)
        assertTrue(vm.canGoNext.value)
        vm.nextDay()
        vm.nextDay()
        assertEquals(LocalDate(2026, 10, 24), vm.date.value)
        assertFalse(vm.canGoNext.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `picking a day shows it and a later day is shown as today`() = runTest {
        val vm = viewModel()
        vm.pickDay(LocalDate(2026, 10, 3))
        assertEquals(LocalDate(2026, 10, 3), vm.date.value)
        vm.pickDay(LocalDate(2026, 11, 3))
        assertEquals(LocalDate(2026, 10, 24), vm.date.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the default follows the date past midnight but a choice stays`() = runTest {
        val vm = viewModel()
        today.value = LocalDate(2026, 10, 25)
        assertEquals(LocalDate(2026, 10, 25), vm.date.value)
        vm.pickDay(LocalDate(2026, 10, 20))
        today.value = LocalDate(2026, 10, 26)
        assertEquals(LocalDate(2026, 10, 20), vm.date.value)
        vm.viewModelScope.cancel()
        today.value = LocalDate(2026, 10, 24)
    }

    @Test
    fun `lists only the redemptions of the shown day newest first and knows the days with some`() = runTest {
        redeem("Old", "2026-10-22T09:00:00Z")
        redeem("Morning", "2026-10-24T08:00:00Z")
        redeem("Evening", "2026-10-24T18:00:00Z")
        val vm = viewModel()

        assertEquals(listOf("Evening", "Morning"), vm.redemptions.value.map { it.name })
        assertEquals(listOf("About Evening", "About Morning"), vm.redemptions.value.map { it.description })
        assertEquals(setOf(LocalDate(2026, 10, 22), LocalDate(2026, 10, 24)), vm.daysWithRedemptions.value)

        vm.pickDay(LocalDate(2026, 10, 22))
        assertEquals(listOf("Old"), vm.redemptions.value.map { it.name })
        vm.pickDay(LocalDate(2026, 10, 23))
        assertTrue(vm.redemptions.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a redemption belongs to its local day also in a zone far from UTC`() = runTest {
        // 23:30 UTC on the 23rd is the 24th in Kiritimati (+14) and 12:30 on the 23rd in Pago Pago (-11).
        redeem("Late", "2026-10-23T23:30:00Z")
        redeem("Early", "2026-10-24T00:30:00Z")

        val east = viewModel(TimeZone.of("Pacific/Kiritimati"))
        assertEquals(setOf(LocalDate(2026, 10, 24)), east.daysWithRedemptions.value)
        assertEquals(listOf("Early", "Late"), east.redemptions.value.map { it.name })

        val utc = viewModel(TimeZone.UTC)
        assertEquals(setOf(LocalDate(2026, 10, 23), LocalDate(2026, 10, 24)), utc.daysWithRedemptions.value)
        assertEquals(listOf("Early"), utc.redemptions.value.map { it.name })
        utc.pickDay(LocalDate(2026, 10, 23))
        assertEquals(listOf("Late"), utc.redemptions.value.map { it.name })

        east.viewModelScope.cancel()
        utc.viewModelScope.cancel()
    }

    @Test
    fun `browsing changes no coins`() = runTest {
        redeem("Tea", "2026-10-23T09:00:00Z")
        val before = store.entries.first()
        val vm = viewModel()
        vm.previousDay()
        vm.pickDay(LocalDate(2026, 10, 23))
        vm.nextDay()
        assertEquals(before, store.entries.first())
        vm.viewModelScope.cancel()
    }
}
