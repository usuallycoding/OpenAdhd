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

    @Test
    fun `entries start expanded`() = runTest {
        val template = store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B"))
        val vm = viewModel()
        vm.assign(template)
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `toggling collapses and toggling again expands`() = runTest {
        val vm = viewModel()
        vm.toggleCollapsed("entry-1")
        assertEquals(setOf("entry-1"), vm.collapsedEntryIds.value)
        vm.toggleCollapsed("entry-1")
        assertTrue(vm.collapsedEntryIds.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `each entry has its own state`() = runTest {
        val vm = viewModel()
        vm.toggleCollapsed("one")
        vm.toggleCollapsed("two")
        vm.toggleCollapsed("one")
        assertEquals(setOf("two"), vm.collapsedEntryIds.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a fresh view model starts with everything expanded`() = runTest {
        val first = viewModel()
        first.toggleCollapsed("entry")
        first.viewModelScope.cancel()

        val afterRestart = viewModel()
        assertTrue(afterRestart.collapsedEntryIds.value.isEmpty())
        afterRestart.viewModelScope.cancel()
    }
}
