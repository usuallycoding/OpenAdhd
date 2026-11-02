package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.today.CriticalHitEvent
import com.usuallycoding.openadhd.ui.templates.TemplateInputError
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import com.usuallycoding.openadhd.ui.wallet.WalletViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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

        today.completeTask(today.entries.value.single().id)
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

    @Test
    fun `input is validated for name subtask titles and the tier's subtask limit`() {
        assertEquals(null, TemplatesViewModel.validate("Clean", Tier.One, listOf("Dishes")))
        assertEquals(TemplateInputError.NameRequired, TemplatesViewModel.validate("  ", Tier.One, emptyList()))
        assertEquals(TemplateInputError.SubtaskTitleRequired, TemplatesViewModel.validate("Clean", Tier.One, listOf("Dishes", " ")))
        assertEquals(null, TemplatesViewModel.validate("Clean", Tier.One, List(5) { "Step" }))
        assertEquals(TemplateInputError.TooManySubtasks, TemplatesViewModel.validate("Clean", Tier.One, List(6) { "Step" }))
        assertEquals(null, TemplatesViewModel.validate("Clean", Tier.Two, List(6) { "Step" }))
        assertEquals(null, TemplatesViewModel.validate("Clean", Tier.Four, List(10) { "Step" }))
        assertEquals(TemplateInputError.TooManySubtasks, TemplatesViewModel.validate("Clean", Tier.Four, List(11) { "Step" }))
    }

    @Test
    fun `templates view model saves description and subtasks and ignores invalid ones`() = runTest {
        val vm = TemplatesViewModel(store)
        vm.save(null, "Clean", Tier.Three, Energy.Low, "Whole room", listOf("Dishes", "Counters"))
        val saved = vm.templates.value.single()
        assertEquals("Whole room", saved.description)
        assertEquals(listOf("Dishes", "Counters"), saved.subtasks)

        vm.save(saved.id, "Clean", Tier.Three, Energy.Low, "Whole room", listOf("Dishes", ""))
        vm.save(null, "Too many", Tier.One, Energy.Low, "", List(6) { "Step" })
        assertEquals(listOf(saved), vm.templates.value)
    }

    @Test
    fun `today view model shows partial progress and applies the removal rule`() = runTest {
        val template = store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C"))
        val today = TodayViewModel(store, clock, TimeZone.UTC)
        val wallet = WalletViewModel(store)

        today.assign(template)
        today.assign(template)
        val (started, untouched) = today.entries.value
        today.completeSubtask(started.subtasks.first().id)

        assertEquals(7, wallet.balance.value)
        assertTrue(today.entries.value.first().hasProgress)
        assertFalse(today.entries.value.first().isCompleted)

        today.remove(started.id)
        today.remove(untouched.id)
        assertEquals(listOf(started.id), today.entries.value.map { it.id })

        today.completeTask(started.id)
        assertEquals(7, wallet.balance.value)

        started.subtasks.drop(1).forEach { today.completeSubtask(it.id) }
        assertTrue(today.entries.value.single().isCompleted)
        assertEquals(20, wallet.balance.value)
        today.viewModelScope.cancel()
    }

    // ---- critical hit events

    @Test
    fun `a critical hit emits an event with the bonus and usual completions say nothing`() = runTest {
        val crits = InMemoryStore(clock, criticalHits = ScriptedCrits(true, false))
        val template = crits.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C"))
        val today = TodayViewModel(crits, clock, TimeZone.UTC, dateCheckInterval = null)
        val events = mutableListOf<CriticalHitEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { today.criticalHits.toList(events) }

        today.assign(template)
        val subtasks = today.entries.value.single().subtasks
        today.completeSubtask(subtasks[0].id)
        today.completeSubtask(subtasks[1].id)

        assertEquals(listOf(CriticalHitEvent(7)), events)
        today.viewModelScope.cancel()
    }

    @Test
    fun `quick successive critical hits each get an event and a task can be critical too`() = runTest {
        val crits = InMemoryStore(clock, criticalHits = ScriptedCrits(true, true, true))
        val template = crits.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("A", "B"))
        val plain = crits.create("Taxes", Tier.Three, Energy.High)
        val today = TodayViewModel(crits, clock, TimeZone.UTC, dateCheckInterval = null)
        val events = mutableListOf<CriticalHitEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { today.criticalHits.toList(events) }

        today.assign(template)
        today.assign(plain)
        val (withSubtasks, withoutSubtasks) = today.entries.value
        today.completeSubtask(withSubtasks.subtasks[0].id)
        today.completeSubtask(withSubtasks.subtasks[1].id)
        today.completeTask(withoutSubtasks.id)

        assertEquals(listOf(CriticalHitEvent(5), CriticalHitEvent(5), CriticalHitEvent(20)), events)
        today.viewModelScope.cancel()
    }

    @Test
    fun `refused completions never emit an event and never roll`() = runTest {
        val crits = ScriptedCrits()
        val noRolls = InMemoryStore(clock, criticalHits = crits)
        val template = noRolls.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("A"))
        val today = TodayViewModel(noRolls, clock, TimeZone.UTC, dateCheckInterval = null)
        val events = mutableListOf<CriticalHitEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { today.criticalHits.toList(events) }

        today.assign(template)
        today.completeTask(today.entries.value.single().id)
        today.completeSubtask("missing")

        assertTrue(events.isEmpty())
        today.viewModelScope.cancel()
    }

    @Test
    fun `a create request stays set until it is consumed`() {
        val vm = TemplatesViewModel(store)
        assertFalse(vm.createRequested.value)
        vm.requestCreate()
        assertTrue(vm.createRequested.value)
        vm.consumeCreateRequest()
        assertFalse(vm.createRequested.value)
        vm.viewModelScope.cancel()
    }
}
