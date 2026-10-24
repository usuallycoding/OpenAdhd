package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReorderViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun todayViewModel() = TodayViewModel(store, clock, TimeZone.UTC, dateCheckInterval = null)

    // ---- task templates

    @Test
    fun `task templates move up and down and the order is saved`() = runTest {
        val a = store.create("A", Tier.One, Energy.Low)
        val b = store.create("B", Tier.One, Energy.Low)
        val c = store.create("C", Tier.One, Energy.Low)
        val vm = TemplatesViewModel(store)
        vm.moveDown(a.id)
        assertEquals(listOf("B", "A", "C"), vm.templates.value.map { it.name })
        vm.moveUp(c.id)
        assertEquals(listOf("B", "C", "A"), vm.templates.value.map { it.name })
        assertEquals(listOf("B", "C", "A"), store.templates.first().map { it.name })
        vm.moveUp(b.id)
        assertEquals(listOf("B", "C", "A"), vm.templates.value.map { it.name })
        vm.moveDown(a.id)
        assertEquals(listOf("B", "C", "A"), vm.templates.value.map { it.name })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `task template reorder mode needs two items and ends when fewer remain`() = runTest {
        val a = store.create("A", Tier.One, Energy.Low)
        val vm = TemplatesViewModel(store)
        vm.setReordering(true)
        assertFalse(vm.reordering.value)
        val b = store.create("B", Tier.One, Energy.Low)
        assertTrue(vm.reordering.value)
        vm.setReordering(false)
        assertFalse(vm.reordering.value)
        vm.setReordering(true)
        store.delete(b.id)
        assertFalse(vm.reordering.value)
        assertEquals(listOf(a.id), vm.templates.value.map { it.id })
        vm.viewModelScope.cancel()
    }

    // ---- reward templates

    @Test
    fun `reward templates move and the mode follows the item count`() = runTest {
        val a = store.createReward("A", RewardTier.One, Energy.Low)
        store.createReward("B", RewardTier.Two, Energy.Low)
        val vm = RewardTemplatesViewModel(store)
        vm.setReordering(true)
        assertTrue(vm.reordering.value)
        vm.moveDown(a.id)
        assertEquals(listOf("B", "A"), vm.rewards.value.map { it.name })
        vm.moveDown(a.id)
        assertEquals(listOf("B", "A"), vm.rewards.value.map { it.name })
        vm.viewModelScope.cancel()
    }

    // ---- Today

    @Test
    fun `pending entries move among themselves and completed ones stay below`() = runTest {
        val vm = todayViewModel()
        val day = vm.today.value
        val a = store.assign(store.create("A", Tier.One, Energy.Low), day)
        val b = store.assign(store.create("B", Tier.One, Energy.Low), day)
        val c = store.assign(store.create("C", Tier.One, Energy.Low), day)
        store.completeTask(a.id)
        assertEquals(listOf("B", "C", "A"), vm.entries.value.map { it.name })
        vm.moveDown(b.id)
        assertEquals(listOf("C", "B", "A"), vm.entries.value.map { it.name })
        vm.moveDown(b.id)
        assertEquals(listOf("C", "B", "A"), vm.entries.value.map { it.name })
        vm.moveUp(a.id)
        assertEquals(listOf("C", "B", "A"), vm.entries.value.map { it.name })
        assertEquals(c.id, vm.entries.value.first().id)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `completing an entry sends it below the pending ones`() = runTest {
        val vm = todayViewModel()
        val day = vm.today.value
        val a = store.assign(store.create("A", Tier.One, Energy.Low), day)
        store.assign(store.create("B", Tier.One, Energy.Low), day)
        store.assign(store.create("C", Tier.One, Energy.Low), day)
        vm.completeTask(a.id)
        assertEquals(listOf("B", "C", "A"), vm.entries.value.map { it.name })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a new entry goes last among the pending ones`() = runTest {
        val vm = todayViewModel()
        val day = vm.today.value
        val a = store.assign(store.create("A", Tier.One, Energy.Low), day)
        store.assign(store.create("B", Tier.One, Energy.Low), day)
        store.completeTask(a.id)
        vm.assign(store.create("C", Tier.One, Energy.Low))
        assertEquals(listOf("B", "C", "A"), vm.entries.value.map { it.name })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `today reorder mode needs two pending entries`() = runTest {
        val vm = todayViewModel()
        val day = vm.today.value
        val a = store.assign(store.create("A", Tier.One, Energy.Low), day)
        val b = store.assign(store.create("B", Tier.One, Energy.Low), day)
        vm.setReordering(true)
        assertTrue(vm.reordering.value)
        store.completeTask(a.id)
        assertFalse(vm.reordering.value)
        assertEquals(listOf(b.id, a.id), vm.entries.value.map { it.id })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `reordering keeps the collapsed state`() = runTest {
        val vm = todayViewModel()
        val day = vm.today.value
        val a = store.assign(store.create("A", Tier.Two, Energy.Low, subtasks = listOf("x", "y")), day)
        store.assign(store.create("B", Tier.One, Energy.Low), day)
        vm.toggleCollapsed(a.id)
        vm.moveDown(a.id)
        assertEquals(setOf(a.id), vm.collapsedEntryIds.value)
        vm.viewModelScope.cancel()
    }
}
