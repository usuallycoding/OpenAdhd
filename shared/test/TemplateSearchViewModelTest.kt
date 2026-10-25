package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.matchingRewards
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RewardSearchTest {
    private fun reward(name: String, description: String = "") = RewardTemplate(name, name, RewardTier.One, Energy.Low, description)

    private val all = listOf(reward("Movie night", "with snacks"), reward("Tea"), reward("New headphones", "for the bus"))

    private fun names(query: String) = all.matchingRewards(query).map { it.name }

    @Test
    fun `a blank query lists every reward in order`() {
        assertEquals(listOf("Movie night", "Tea", "New headphones"), names(""))
        assertEquals(listOf("Movie night", "Tea", "New headphones"), names("   "))
    }

    @Test
    fun `matches the name ignoring case and outer spaces`() {
        assertEquals(listOf("Tea"), names("  TEA "))
        assertEquals(listOf("Movie night"), names("night"))
    }

    @Test
    fun `matches the description too`() {
        assertEquals(listOf("Movie night"), names("snacks"))
        assertEquals(listOf("New headphones"), names("BUS"))
    }

    @Test
    fun `keeps the order and lists nothing when nothing matches`() {
        assertEquals(listOf("Movie night", "New headphones"), names("o"))
        assertTrue(names("xyz").isEmpty())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateSearchViewModelTest {
    private val store = InMemoryStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun tasks() {
        store.create("Taxes", Tier.One, Energy.Low)
        store.create("Dishes", Tier.One, Energy.Low, "wash up")
        store.create("Laundry", Tier.One, Energy.Low)
    }

    private suspend fun rewards() {
        store.createReward("Cake", RewardTier.One, Energy.Low)
        store.createReward("Tea", RewardTier.One, Energy.Low, "green")
        store.createReward("Movie", RewardTier.One, Energy.Low)
    }

    @Test
    fun `the task list follows the search and keeps its order`() = runTest {
        tasks()
        val vm = TemplatesViewModel(store)
        assertEquals(listOf("Laundry", "Dishes", "Taxes"), vm.visible.value.map { it.name })
        vm.setQuery("a")
        assertEquals(listOf("Laundry", "Dishes", "Taxes"), vm.visible.value.map { it.name })
        vm.setQuery("wash")
        assertEquals(listOf("Dishes"), vm.visible.value.map { it.name })
        vm.setQuery("")
        assertEquals(3, vm.visible.value.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the search text is kept by the view model and the full list is untouched`() = runTest {
        tasks()
        val vm = TemplatesViewModel(store)
        vm.setQuery("dis")
        assertEquals("dis", vm.query.value)
        assertEquals(3, vm.templates.value.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `saving a new task template clears the search but editing and deleting keep it`() = runTest {
        tasks()
        val vm = TemplatesViewModel(store)
        vm.setQuery("dis")
        val dishes = vm.visible.value.single()

        vm.save(dishes.id, "Dishes and pans", Tier.One, Energy.Low)
        assertEquals("dis", vm.query.value)
        vm.delete(dishes.id)
        assertEquals("dis", vm.query.value)

        vm.save(null, "Taxes 2", Tier.One, Energy.Low)
        assertEquals("", vm.query.value)
        assertTrue(vm.visible.value.any { it.name == "Taxes 2" })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `reordering is unavailable while the task search holds text and typing ends it`() = runTest {
        tasks()
        val vm = TemplatesViewModel(store)
        assertTrue(vm.canReorder.value)
        vm.setReordering(true)
        assertTrue(vm.reordering.value)

        vm.setQuery("a")
        assertFalse(vm.canReorder.value)
        assertFalse(vm.reordering.value)

        vm.setQuery("")
        assertTrue(vm.canReorder.value)
        assertFalse(vm.reordering.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a search of only spaces does not hide the toggle`() = runTest {
        tasks()
        val vm = TemplatesViewModel(store)
        vm.setQuery("   ")
        assertTrue(vm.canReorder.value)
        assertEquals(3, vm.visible.value.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the reward list follows the search`() = runTest {
        rewards()
        val vm = RewardTemplatesViewModel(store)
        assertEquals(listOf("Movie", "Tea", "Cake"), vm.visible.value.map { it.name })
        vm.setQuery("green")
        assertEquals(listOf("Tea"), vm.visible.value.map { it.name })
        vm.setQuery("MOV")
        assertEquals(listOf("Movie"), vm.visible.value.map { it.name })
        assertEquals(3, vm.rewards.value.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `saving a new reward clears the search but editing and deleting keep it`() = runTest {
        rewards()
        val vm = RewardTemplatesViewModel(store)
        vm.setQuery("tea")
        val tea = vm.visible.value.single()

        vm.save(tea.id, "Tea time", RewardTier.One, Energy.Low)
        assertEquals("tea", vm.query.value)
        vm.delete(tea.id)
        assertEquals("tea", vm.query.value)

        vm.save(null, "Pie", RewardTier.One, Energy.Low)
        assertEquals("", vm.query.value)
        assertTrue(vm.visible.value.any { it.name == "Pie" })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `reordering is unavailable while the reward search holds text`() = runTest {
        rewards()
        val vm = RewardTemplatesViewModel(store)
        vm.setReordering(true)
        assertTrue(vm.reordering.value)
        vm.setQuery("e")
        assertFalse(vm.canReorder.value)
        assertFalse(vm.reordering.value)
        vm.setQuery("")
        assertTrue(vm.canReorder.value)
        assertFalse(vm.reordering.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `each tab's search is its own`() = runTest {
        tasks()
        rewards()
        val taskVm = TemplatesViewModel(store)
        val rewardVm = RewardTemplatesViewModel(store)
        taskVm.setQuery("dis")
        assertEquals("", rewardVm.query.value)
        assertTrue(rewardVm.canReorder.value)
        assertFalse(taskVm.canReorder.value)
        taskVm.viewModelScope.cancel()
        rewardVm.viewModelScope.cancel()
    }
}
