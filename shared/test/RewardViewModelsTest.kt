package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import com.usuallycoding.openadhd.ui.shop.buildShelves
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
import kotlin.test.assertTrue
import com.usuallycoding.openadhd.domain.RewardTemplate

@OptIn(ExperimentalCoroutinesApi::class)
class RewardViewModelsTest {
    private val clock = FakeClock()
    private val store = InMemoryStore(clock)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun earn(tier: Tier) {
        val entry = store.assign(store.create("Earn ${tier.level}", tier, Energy.Low), clock.todayIn(TimeZone.UTC))
        store.completeTask(entry.id)
    }

    private fun reward(id: String, tier: RewardTier, customPrice: Int? = null, name: String = id) =
        RewardTemplate(id, name, tier, Energy.Low, customPrice = customPrice)

    // ---- reward templates

    @Test
    fun `reward input is validated`() {
        assertEquals(null, RewardTemplatesViewModel.validate("Movie night", RewardTier.Two, null))
        assertEquals(RewardInputError.NameRequired, RewardTemplatesViewModel.validate(" ", RewardTier.Two, null))
        assertEquals(RewardInputError.PriceRequired, RewardTemplatesViewModel.validate("Gift", RewardTier.Five, null))
        assertEquals(RewardInputError.PriceTooLow, RewardTemplatesViewModel.validate("Gift", RewardTier.Five, 300))
        assertEquals(null, RewardTemplatesViewModel.validate("Gift", RewardTier.Five, 301))
    }

    @Test
    fun `reward view model creates edits and deletes and ignores invalid input`() = runTest {
        val vm = RewardTemplatesViewModel(store)
        vm.save(null, "Movie night", RewardTier.Two, Energy.Low, "Snacks")
        val saved = vm.rewards.value.single()
        assertEquals(50, saved.price)

        vm.save(saved.id, "Movie night", RewardTier.Five, Energy.Low, "Snacks", 300)
        assertEquals(RewardTier.Two, vm.rewards.value.single().tier)

        vm.save(saved.id, "Cinema trip", RewardTier.Five, Energy.High, "", 450)
        assertEquals(450, vm.rewards.value.single().price)

        vm.save(null, " ", RewardTier.One, Energy.Low)
        assertEquals(1, vm.rewards.value.size)

        vm.delete(saved.id)
        assertTrue(vm.rewards.value.isEmpty())
        vm.viewModelScope.cancel()
    }

    // ---- shelves

    @Test
    fun `shelves are grouped by tier in order and skip empty tiers`() {
        val shelves = buildShelves(
            listOf(reward("t5", RewardTier.Five, 500), reward("t1", RewardTier.One), reward("t2", RewardTier.Two)),
            balance = 0,
        )
        assertEquals(listOf(RewardTier.One, RewardTier.Two, RewardTier.Five), shelves.map { it.tier })
    }

    @Test
    fun `tier five is ordered by price and other tiers keep their order`() {
        val shelves = buildShelves(
            listOf(
                reward("dear", RewardTier.Five, 900),
                reward("cheap", RewardTier.Five, 500),
                reward("b", RewardTier.One),
                reward("a", RewardTier.One),
            ),
            balance = 0,
        )
        assertEquals(listOf("b", "a"), shelves.first { it.tier == RewardTier.One }.items.map { it.reward.id })
        assertEquals(listOf("cheap", "dear"), shelves.first { it.tier == RewardTier.Five }.items.map { it.reward.id })
    }

    @Test
    fun `affordability shows how many coins are still needed`() {
        val items = buildShelves(listOf(reward("r", RewardTier.Two)), balance = 30).single().items
        assertFalse(items.single().affordable)
        assertEquals(20, items.single().coinsNeeded)

        val exact = buildShelves(listOf(reward("r", RewardTier.Two)), balance = 50).single().items.single()
        assertTrue(exact.affordable)
        assertEquals(0, exact.coinsNeeded)
    }

    // ---- shop

    @Test
    fun `shop view model redeems records history and refuses to overdraw`() = runTest {
        earn(Tier.Four)
        earn(Tier.Four)
        val cheap = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        val dear = store.createReward("Dinner out", RewardTier.Three, Energy.High)
        val vm = ShopViewModel(store)

        assertEquals(80, vm.balance.value)
        assertEquals(listOf(true, false), vm.shelves.value.flatMap { it.items }.map { it.affordable })

        vm.redeem(cheap.id, "r1")
        assertEquals(30, vm.balance.value)
        assertEquals(listOf("Movie night"), vm.history.value.map { it.name })
        assertEquals(false, vm.shelves.value.flatMap { it.items }.any { it.affordable && it.reward.id == cheap.id })

        vm.redeem(dear.id, "r2")
        assertEquals(30, vm.balance.value)
        assertEquals(1, vm.history.value.size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `confirming twice with the same redemption id spends once`() = runTest {
        repeat(2) { earn(Tier.Four) }
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        val vm = ShopViewModel(store)

        vm.redeem(reward.id, "same")
        vm.redeem(reward.id, "same")

        assertEquals(30, vm.balance.value)
        assertEquals(1, vm.history.value.size)
        vm.viewModelScope.cancel()
    }
}
