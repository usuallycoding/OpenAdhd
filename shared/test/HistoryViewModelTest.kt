package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.history.HistoryViewModel
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val today = MutableStateFlow(LocalDate(2026, 10, 24))
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HistoryViewModel(store, today)

    private suspend fun add(name: String, day: LocalDate, vararg subtasks: String) =
        store.assign(store.create(name, Tier.Three, Energy.Low, subtasks = subtasks.toList()), day)

    @Test
    fun `starts on yesterday and cannot go to a later day`() = runTest {
        val vm = viewModel()
        assertEquals(LocalDate(2026, 10, 23), vm.date.value)
        assertFalse(vm.canGoNext.value)
        vm.nextDay()
        assertEquals(LocalDate(2026, 10, 23), vm.date.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `moving back and forth stays before today`() = runTest {
        val vm = viewModel()
        vm.previousDay()
        vm.previousDay()
        assertEquals(LocalDate(2026, 10, 21), vm.date.value)
        assertTrue(vm.canGoNext.value)
        vm.nextDay()
        assertEquals(LocalDate(2026, 10, 22), vm.date.value)
        vm.nextDay()
        assertFalse(vm.canGoNext.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `picking a day shows it and today or later is shown as yesterday`() = runTest {
        val vm = viewModel()
        vm.pickDay(LocalDate(2026, 10, 1))
        assertEquals(LocalDate(2026, 10, 1), vm.date.value)
        vm.pickDay(LocalDate(2026, 10, 24))
        assertEquals(LocalDate(2026, 10, 23), vm.date.value)
        vm.pickDay(LocalDate(2026, 11, 3))
        assertEquals(LocalDate(2026, 10, 23), vm.date.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the default follows the date past midnight but a choice stays`() = runTest {
        val vm = viewModel()
        today.value = LocalDate(2026, 10, 25)
        assertEquals(LocalDate(2026, 10, 24), vm.date.value)
        vm.pickDay(LocalDate(2026, 10, 20))
        today.value = LocalDate(2026, 10, 26)
        assertEquals(LocalDate(2026, 10, 20), vm.date.value)
        vm.viewModelScope.cancel()
        today.value = LocalDate(2026, 10, 24)
    }

    @Test
    fun `lists the entries of the shown day only with pending first and then done newest first`() = runTest {
        val yesterday = LocalDate(2026, 10, 23)
        val a = add("A", yesterday)
        val b = add("B", yesterday)
        add("Pending", yesterday)
        add("Elsewhere", LocalDate(2026, 10, 22))
        add("Today", LocalDate(2026, 10, 24))
        clock.current = Instant.parse("2026-10-23T10:00:00Z")
        store.completeTask(a.id)
        clock.current = Instant.parse("2026-10-23T11:00:00Z")
        store.completeTask(b.id)
        val vm = viewModel()
        assertEquals(listOf("Pending", "B", "A"), vm.entries.value.map { it.name })
        vm.pickDay(LocalDate(2026, 10, 22))
        assertEquals(listOf("Elsewhere"), vm.entries.value.map { it.name })
        vm.viewModelScope.cancel()
        clock.current = Instant.parse("2026-10-23T10:00:00Z")
    }

    @Test
    fun `collapse defaults are expanded when pending and collapsed when done and toggling flips them`() = runTest {
        val yesterday = LocalDate(2026, 10, 23)
        val pending = add("Pending", yesterday, "A", "B")
        val done = add("Done", yesterday, "A")
        store.completeSubtask(done.subtasks.single().id)
        val vm = viewModel()
        assertEquals(setOf(done.id), vm.collapsedEntryIds.value)
        vm.toggleCollapsed(done.id)
        vm.toggleCollapsed(pending.id)
        assertEquals(setOf(pending.id), vm.collapsedEntryIds.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `days with entries come from the store and browsing changes no balance`() = runTest {
        add("Old", LocalDate(2026, 10, 20))
        val vm = viewModel()
        assertEquals(setOf(LocalDate(2026, 10, 20)), vm.daysWithEntries.value)
        val before = store.entries.first()
        vm.previousDay()
        vm.pickDay(LocalDate(2026, 10, 20))
        vm.toggleCollapsed("x")
        assertEquals(before, store.entries.first())
        vm.viewModelScope.cancel()
    }
}
