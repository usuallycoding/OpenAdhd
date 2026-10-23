package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import com.usuallycoding.openadhd.ui.wallet.WalletViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val clock = FakeClock(Instant.parse("2026-10-23T10:00:00Z"))
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `blank names are not valid`() {
        assertFalse(TemplatesViewModel.isValidName("   "))
        assertTrue(TemplatesViewModel.isValidName("Dishes"))
    }

    @Test
    fun `templates view model creates edits and deletes`() = runTest {
        val vm = TemplatesViewModel(store)
        vm.save(null, "Dishes", Tier.One, Energy.Low)
        val created = vm.templates.value.single()
        assertEquals("Dishes", created.name)

        vm.save(created.id, "Dishes and pans", Tier.Two, Energy.Medium)
        assertEquals(Tier.Two, vm.templates.value.single().tier)

        vm.delete(created.id)
        assertTrue(vm.templates.value.isEmpty())
    }

    @Test
    fun `templates view model ignores a blank name`() = runTest {
        val vm = TemplatesViewModel(store)
        vm.save(null, "  ", Tier.One, Energy.Low)
        assertTrue(vm.templates.value.isEmpty())
    }

    @Test
    fun `today view model assigns removes and completes`() = runTest {
        val template = store.create("Walk", Tier.Three, Energy.Medium)
        val today = TodayViewModel(store, clock, TimeZone.UTC)
        val wallet = WalletViewModel(store)

        today.assign(template)
        today.assign(template)
        assertEquals(2, today.entries.value.size)

        today.remove(today.entries.value.first().id)
        assertEquals(1, today.entries.value.size)

        today.complete(today.entries.value.single().id)
        assertTrue(today.entries.value.single().isCompleted)
        assertEquals(20, wallet.balance.value)
        today.viewModelScope.cancel()
    }

    @Test
    fun `today list rolls over when the day changes`() = runTest {
        val template = store.create("Walk", Tier.One, Energy.Low)
        val today = TodayViewModel(store, clock, TimeZone.UTC)
        today.assign(template)
        assertEquals(LocalDate(2026, 10, 23), today.today.value)
        assertEquals(1, today.entries.value.size)

        clock.current = Instant.parse("2026-10-24T00:30:00Z")
        testScheduler.advanceTimeBy(31.seconds)
        testScheduler.runCurrent()

        assertEquals(LocalDate(2026, 10, 24), today.today.value)
        assertTrue(today.entries.value.isEmpty())
        today.viewModelScope.cancel()
    }
}
