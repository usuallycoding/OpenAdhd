package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.GameRules
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import com.usuallycoding.openadhd.ui.shop.buildRollOptions
import com.usuallycoding.openadhd.ui.shop.buildShelves
import com.usuallycoding.openadhd.ui.templates.TemplateInputError
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the screens show and allow when the settings are not the defaults. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangedRulesViewModelTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun InMemoryStore.rulesNow(): GameRules = settings.first().rules

    private suspend fun InMemoryStore.templatesNow() = templates.first()

    private fun reward(id: String, tier: RewardTier) = RewardTemplate(id, id, tier, Energy.Low)

    @Test
    fun `shelves and rolls use the debt limit and the changed prices`() {
        val rules = GameRules(debtLimit = 100, rewardPrices = listOf(20, 60, 120, 300))
        val rewards = listOf(reward("tea", RewardTier.Two).priced(rules))

        val shelf = buildShelves(rewards, balance = -50, rules = rules).single().items.single()
        assertEquals(60, shelf.reward.price)
        assertFalse(shelf.affordable)
        assertEquals(10, shelf.coinsNeeded)

        val roll = buildRollOptions(rewards, balance = -40, rules = rules).first { it.tier == RewardTier.Two }
        assertEquals(60, roll.price)
        assertTrue(roll.canRoll)
        assertEquals(0, roll.coinsNeeded)
    }

    @Test
    fun `the shop follows a changed debt limit and a changed price`() = runTest {
        store.createReward("Tea", RewardTier.Two, Energy.Low)
        val vm = ShopViewModel(store)
        assertEquals(listOf(true), vm.shelves.value.flatMap { it.items }.map { it.affordable })

        assertNull(store.saveSetting(SettingKey.DebtLimit, "10"))
        assertEquals(listOf(false), vm.shelves.value.flatMap { it.items }.map { it.affordable })
        assertEquals(40, vm.shelves.value.single().items.single().coinsNeeded)

        assertNull(store.saveSetting(SettingKey.PriceTier2, "30"))
        assertEquals(30, vm.shelves.value.single().items.single().reward.price)
        assertEquals(20, vm.rollOptions.value.first { it.tier == RewardTier.Two }.coinsNeeded)

        store.restoreSetting(SettingKey.DebtLimit)
        store.restoreSetting(SettingKey.PriceTier2)
        assertEquals(50, vm.shelves.value.single().items.single().reward.price)
        assertTrue(vm.shelves.value.single().items.single().affordable)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the template view model validates against the changed ceiling and payouts`() = runTest {
        val vm = TemplatesViewModel(store, store.settings)
        val eight = List(8) { "Step $it" }
        assertNull(TemplatesViewModel.validate("Clean", Tier.Two, eight))

        assertNull(store.saveSetting(SettingKey.SubtaskCeiling, "5"))
        assertEquals(TemplateInputError.TooManySubtasks, TemplatesViewModel.validate("Clean", Tier.Two, eight, store.rulesNow()))

        vm.save(null, "Clean", Tier.Two, Energy.Low, subtasks = eight)
        assertTrue(store.templatesNow().isEmpty())
        vm.save(null, "Clean", Tier.Two, Energy.Low, subtasks = eight.take(5))
        assertEquals(5, store.templatesNow().single().subtasks.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `the reward view model validates against the changed tier four price`() = runTest {
        val vm = RewardTemplatesViewModel(store, store.settings)
        assertNull(store.saveSetting(SettingKey.PriceTier4, "400"))
        assertEquals(RewardInputError.PriceTooLow, RewardTemplatesViewModel.validate("Trip", RewardTier.Five, 400, store.rulesNow()))

        vm.save(null, "Trip", RewardTier.Five, Energy.High, customPrice = 400)
        assertTrue(vm.rewards.value.isEmpty())
        vm.save(null, "Trip", RewardTier.Five, Energy.High, customPrice = 401)
        assertEquals(401, vm.rewards.value.single().price)
        vm.viewModelScope.cancel()
    }
}
