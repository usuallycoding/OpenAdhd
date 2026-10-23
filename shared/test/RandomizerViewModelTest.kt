package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.RandomizerState
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RandomizerViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg draws: Int) =
        TodayViewModel(store, clock, TimeZone.UTC, dateCheckInterval = null, random = ScriptedRandom(*draws))

    private suspend fun template(name: String, energy: Energy, tier: Tier = Tier.Two): TaskTemplate =
        store.create(name, tier, energy)

    private fun TodayViewModel.suggested(): String? = (randomizer.value as? RandomizerState.Suggesting)?.template?.name

    @Test
    fun `no templates at all says so`() = runTest {
        val vm = viewModel()
        vm.openRandomizer()
        assertEquals(RandomizerState.NoTemplates, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `with templates it asks for the energy first`() = runTest {
        template("Dishes", Energy.Low)
        val vm = viewModel()
        vm.openRandomizer()
        assertEquals(RandomizerState.ChoosingEnergy, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `choosing an energy suggests a template by weight`() = runTest {
        template("High job", Energy.High)
        template("Medium job", Energy.Medium)
        template("Low job", Energy.Low)
        val vm = viewModel(4)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.High)

        val state = assertIs<RandomizerState.Suggesting>(vm.randomizer.value)
        assertEquals(Energy.High, state.energy)
        assertEquals("Medium job", state.template.name) // draw 4 of 7 falls in the medium weight
        vm.viewModelScope.cancel()
    }

    @Test
    fun `low energy never suggests anything that needs more`() = runTest {
        template("Medium job", Energy.Medium)
        template("High job", Energy.High)
        val vm = viewModel()
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Low)

        assertEquals(RandomizerState.NothingFits(Energy.Low, exhausted = false), vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `templates already on today's list are never suggested`() = runTest {
        val pending = template("Pending", Energy.Medium)
        val done = template("Done", Energy.Medium)
        template("Fresh", Energy.Medium)
        val vm = viewModel(0)
        vm.assign(pending)
        vm.assign(done)
        vm.completeTask(vm.entries.value.first { it.name == "Done" }.id)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Medium)

        assertEquals("Fresh", vm.suggested())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `another never repeats until everything has been shown and then says so`() = runTest {
        template("A", Energy.Medium)
        template("B", Energy.Medium)
        template("C", Energy.Low)
        val vm = viewModel(0, 0, 0)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Medium)
        val first = vm.suggested()
        vm.another()
        val second = vm.suggested()
        vm.another()
        val third = vm.suggested()

        assertEquals(listOf("A", "B", "C"), listOf(first, second, third))
        vm.another()
        assertEquals(RandomizerState.NothingFits(Energy.Medium, exhausted = true), vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `choosing the energy again forgets what was shown`() = runTest {
        template("A", Energy.Medium)
        template("B", Energy.Medium)
        val vm = viewModel(0, 0)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Medium)
        assertEquals("A", vm.suggested())
        vm.chooseEnergy(Energy.Medium)

        assertEquals("A", vm.suggested())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `choose another energy returns to the energy question`() = runTest {
        template("A", Energy.Medium)
        val vm = viewModel(0)
        vm.openRandomizer()
        vm.chooseEnergy(Energy.Medium)
        vm.another()
        assertEquals(RandomizerState.NothingFits(Energy.Medium, exhausted = true), vm.randomizer.value)

        vm.chooseAnotherEnergy()

        assertEquals(RandomizerState.ChoosingEnergy, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `accepting adds the entry with its frozen reward and closes`() = runTest {
        store.create("Clean", Tier.Three, Energy.Medium, "Whole room", listOf("A", "B", "C"))
        val vm = viewModel(0)
        vm.openRandomizer()
        vm.chooseEnergy(Energy.Medium)

        vm.acceptSuggestion()

        val entry = vm.entries.value.single()
        assertEquals("Clean", entry.name)
        assertEquals("Whole room", entry.description)
        assertEquals(20, entry.reward)
        assertEquals(listOf(7, 7, 6), entry.subtasks.map { it.reward })
        assertEquals(RandomizerState.Closed, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `nothing is added by suggesting or dismissing`() = runTest {
        template("A", Energy.Medium)
        val vm = viewModel(0)
        vm.openRandomizer()
        vm.chooseEnergy(Energy.Medium)
        assertTrue(vm.entries.value.isEmpty())

        vm.dismissRandomizer()

        assertTrue(vm.entries.value.isEmpty())
        assertEquals(0, CoinRules.balance(store.entries.first()))
        assertEquals(RandomizerState.Closed, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `another and accept do nothing when no suggestion is showing`() = runTest {
        template("A", Energy.Medium)
        val vm = viewModel()
        vm.another()
        vm.acceptSuggestion()
        assertEquals(RandomizerState.Closed, vm.randomizer.value)
        assertTrue(vm.entries.value.isEmpty())
        vm.viewModelScope.cancel()
    }
}
