package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import com.usuallycoding.openadhd.ui.today.UndoneEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class UncheckViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TodayViewModel(store, clock, TimeZone.UTC, dateCheckInterval = null)

    private suspend fun TodayViewModel.addEntry(name: String, vararg subtasks: String) =
        store.assign(store.create(name, Tier.Three, Energy.Low, subtasks = subtasks.toList()), today.value)

    private fun at(time: String) {
        clock.current = Instant.parse("2026-10-23T${time}:00Z")
    }

    private fun TodayViewModel.names() = entries.value.map { it.name }

    @Test
    fun `done entries come after pending ones and the most recently done is first`() = runTest {
        val vm = viewModel()
        val a = vm.addEntry("A")
        val b = vm.addEntry("B")
        vm.addEntry("C")
        at("10:00"); store.completeTask(a.id)
        at("11:00"); store.completeTask(b.id)
        assertEquals(listOf("C", "B", "A"), vm.names())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `an entry with subtasks is ordered by its latest subtask`() = runTest {
        val vm = viewModel()
        val task = vm.addEntry("Task")
        val entry = vm.addEntry("Entry", "x", "y")
        at("10:00"); store.completeSubtask(entry.subtasks[0].id)
        at("10:15"); store.completeTask(task.id)
        at("10:30"); store.completeSubtask(entry.subtasks[1].id)
        assertEquals(listOf("Entry", "Task"), vm.names())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `unchecking brings an entry back among the pending ones at its stored position`() = runTest {
        val vm = viewModel()
        val a = vm.addEntry("A")
        val b = vm.addEntry("B")
        vm.addEntry("C")
        at("10:00"); store.completeTask(a.id)
        assertEquals(listOf("B", "C", "A"), vm.names())
        vm.uncompleteTask(a.id)
        assertEquals(listOf("A", "B", "C"), vm.names())
        assertEquals(b.id, vm.entries.value[1].id)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `reordering moves only pending entries and leaves the done order by time`() = runTest {
        val vm = viewModel()
        val a = vm.addEntry("A")
        val b = vm.addEntry("B")
        val c = vm.addEntry("C")
        val d = vm.addEntry("D")
        at("10:00"); store.completeTask(a.id)
        at("11:00"); store.completeTask(b.id)
        vm.moveDown(c.id)
        assertEquals(listOf("D", "C", "B", "A"), vm.names())
        assertEquals(d.id, vm.entries.value.first().id)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `unchecking reports the coins taken back and a no-op reports nothing`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Taxes")
        val events = mutableListOf<UndoneEvent>()
        val job = vm.viewModelScope.launch { vm.undone.collect { events += it } }

        vm.uncompleteTask(entry.id)
        assertTrue(events.isEmpty())

        store.completeTask(entry.id)
        vm.uncompleteTask(entry.id)
        assertEquals(listOf(UndoneEvent(20)), events)

        vm.uncompleteTask(entry.id)
        vm.uncompleteSubtask("nope")
        assertEquals(1, events.size)
        job.cancel()
        vm.viewModelScope.cancel()
    }

    @Test
    fun `unchecking a subtask reports its share`() = runTest {
        val vm = viewModel()
        val entry = vm.addEntry("Clean", "A", "B", "C")
        val events = mutableListOf<UndoneEvent>()
        val job = vm.viewModelScope.launch { vm.undone.collect { events += it } }
        vm.completeSubtask(entry.subtasks[0].id)
        vm.uncompleteSubtask(entry.subtasks[0].id)
        assertEquals(listOf(UndoneEvent(7)), events)
        job.cancel()
        vm.viewModelScope.cancel()
    }
}
