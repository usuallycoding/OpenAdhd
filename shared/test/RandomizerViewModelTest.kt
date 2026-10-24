package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.PickedEntryEvent
import com.usuallycoding.openadhd.ui.today.RandomizerState
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class RandomizerViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(vararg draws: Int) =
        TodayViewModel(store, clock, TimeZone.UTC, dateCheckInterval = null, random = ScriptedRandom(*draws))

    /** Plans a new template and returns the entry's id; entries are listed in the order they were planned. */
    private suspend fun TodayViewModel.plan(name: String, energy: Energy): String {
        assign(store.create(name, Tier.Two, energy))
        return entries.value.first { it.name == name }.id
    }

    /** Collects the pick events of [vm] so a test can read what was picked. */
    private fun TestScope.collectPicks(vm: TodayViewModel): List<PickedEntryEvent> {
        val picks = mutableListOf<PickedEntryEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.picked.collect { picks += it } }
        return picks
    }

    @Test
    fun `an empty day has nothing to pick`() = runTest {
        val vm = viewModel()
        vm.openRandomizer()
        assertEquals(RandomizerState.NothingPending, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a day where everything is done has nothing to pick`() = runTest {
        val vm = viewModel()
        val id = vm.plan("Dishes", Energy.Low)
        vm.completeTask(id)

        vm.openRandomizer()

        assertEquals(RandomizerState.NothingPending, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `with pending entries it asks for the energy first`() = runTest {
        val vm = viewModel()
        vm.plan("Dishes", Energy.Low)
        vm.openRandomizer()
        assertEquals(RandomizerState.ChoosingEnergy, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `choosing an energy picks an entry by weight and closes`() = runTest {
        val vm = viewModel(4)
        vm.plan("High job", Energy.High)
        val medium = vm.plan("Medium job", Energy.Medium)
        vm.plan("Low job", Energy.Low)
        val picks = collectPicks(vm)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.High)

        assertEquals(listOf(PickedEntryEvent(medium)), picks) // draw 4 of 7 falls in the medium weight
        assertEquals(RandomizerState.Closed, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `low energy never picks anything that needs more`() = runTest {
        val vm = viewModel()
        vm.plan("Medium job", Energy.Medium)
        vm.plan("High job", Energy.High)
        val picks = collectPicks(vm)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Low)

        assertEquals(RandomizerState.NothingFits(Energy.Low), vm.randomizer.value)
        assertTrue(picks.isEmpty())
        vm.viewModelScope.cancel()
    }

    @Test
    fun `completed entries are never picked`() = runTest {
        val vm = viewModel(0)
        val done = vm.plan("Done", Energy.Medium)
        val pending = vm.plan("Pending", Energy.Medium)
        vm.completeTask(done)
        val picks = collectPicks(vm)
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Medium)

        assertEquals(listOf(PickedEntryEvent(pending)), picks)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `picking again may pick the same entry`() = runTest {
        val vm = viewModel(0, 0)
        val only = vm.plan("A", Energy.Medium)
        val picks = collectPicks(vm)

        vm.openRandomizer()
        vm.chooseEnergy(Energy.Medium)
        vm.openRandomizer()
        vm.chooseEnergy(Energy.Medium)

        assertEquals(listOf(PickedEntryEvent(only), PickedEntryEvent(only)), picks)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `choose another energy returns to the energy question`() = runTest {
        val vm = viewModel()
        vm.plan("A", Energy.Medium)
        vm.openRandomizer()
        vm.chooseEnergy(Energy.Low)
        assertEquals(RandomizerState.NothingFits(Energy.Low), vm.randomizer.value)

        vm.chooseAnotherEnergy()

        assertEquals(RandomizerState.ChoosingEnergy, vm.randomizer.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `picking changes nothing on the list or the balance`() = runTest {
        val vm = viewModel(0)
        vm.plan("A", Energy.Medium)
        vm.plan("B", Energy.Low)
        val before = vm.entries.value
        vm.openRandomizer()

        vm.chooseEnergy(Energy.Medium)

        assertEquals(before, vm.entries.value)
        assertEquals(0, CoinRules.balance(store.entries.first()))
        vm.viewModelScope.cancel()
    }

    @Test
    fun `not now closes the question without picking`() = runTest {
        val vm = viewModel()
        vm.plan("A", Energy.Medium)
        val picks = collectPicks(vm)
        vm.openRandomizer()

        vm.dismissRandomizer()

        assertEquals(RandomizerState.Closed, vm.randomizer.value)
        assertTrue(picks.isEmpty())
        vm.viewModelScope.cancel()
    }
}
