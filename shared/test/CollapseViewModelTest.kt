package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollapseViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TodayViewModel(store, clock, TimeZone.UTC, dateCheckInterval = null)

    private suspend fun TodayViewModel.addEntry(name: String, vararg subtasks: String) =
        store.assign(store.create(name, Tier.Three, Energy.Low, subtasks = subtasks.toList()), today.value)

    @Test
    fun `pending entries start expanded`() = runTest {
        val vm = viewModel()
        vm.addEntry("Clean", "A", "B")
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `toggling collapses and toggling again expands`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Clean", "A", "B")
        vm.toggleCollapsed(entry.id)
        assertEquals(setOf(entry.id), vm.collapsedEntryIds.value)
        vm.toggleCollapsed(entry.id)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `each entry has its own state`() = runTest {
        val vm = viewModel()
        val one = vm.addEntry("One", "A")
        val two = vm.addEntry("Two", "A")
        vm.toggleCollapsed(one.id)
        vm.toggleCollapsed(two.id)
        vm.toggleCollapsed(one.id)
        assertEquals(setOf(two.id), vm.collapsedEntryIds.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `entries without subtasks are never collapsed`() = runTest {
        val vm = viewModel()
        val plain = vm.addEntry("Plain")
        vm.toggleCollapsed(plain.id)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a fresh view model starts with pending entries expanded and done entries collapsed`() = runTest {
        val first = viewModel()
        val done = first.addEntry("Done", "A")
        val pending = first.addEntry("Pending", "A")
        store.completeSubtask(done.subtasks.single().id)
        first.toggleCollapsed(pending.id)
        first.viewModelScope.cancel()

        val afterRestart = viewModel()
        assertEquals(setOf(done.id), afterRestart.collapsedEntryIds.value)
        afterRestart.viewModelScope.cancel()
    }

    @Test
    fun `a completed entry starts collapsed and a tap opens it`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Clean", "A")
        store.completeSubtask(entry.subtasks.single().id)
        assertEquals(setOf(entry.id), vm.collapsedEntryIds.value)
        vm.toggleCollapsed(entry.id)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `finishing the last subtask folds the entry even if the user had collapsed and reopened it`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Clean", "A", "B")
        vm.toggleCollapsed(entry.id)
        vm.toggleCollapsed(entry.id)
        store.completeSubtask(entry.subtasks[0].id)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        store.completeSubtask(entry.subtasks[1].id)
        assertEquals(setOf(entry.id), vm.collapsedEntryIds.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `unchecking opens a completed entry again`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Clean", "A", "B")
        entry.subtasks.forEach { store.completeSubtask(it.id) }
        assertEquals(setOf(entry.id), vm.collapsedEntryIds.value)
        vm.uncompleteSubtask(entry.subtasks[1].id)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a tapped choice lasts until the entry changes between pending and completed`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Clean", "A", "B")
        vm.toggleCollapsed(entry.id)
        store.completeSubtask(entry.subtasks[0].id)
        assertEquals(setOf(entry.id), vm.collapsedEntryIds.value)
        store.completeSubtask(entry.subtasks[1].id)
        vm.toggleCollapsed(entry.id)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }
}
