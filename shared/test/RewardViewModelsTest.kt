package com.usuallycoding.openadhd

import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import com.usuallycoding.openadhd.ui.shop.buildRollOptions
import com.usuallycoding.openadhd.ui.shop.buildShelves
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
    fun `affordability counts the debt limit and shows how many coins are still needed`() {
        val rewards = listOf(reward("r", RewardTier.Two))
        fun item(balance: Int) = buildShelves(rewards, balance).single().items.single()

        // 50 coins with only 30: affordable by borrowing, nothing more is needed
        assertTrue(item(30).affordable)
        assertEquals(0, item(30).coinsNeeded)
        assertTrue(item(50).affordable)
        // exactly at the limit (-450 - 50 = -500) is still fine, one coin beyond is not
        assertTrue(item(-450).affordable)
        assertFalse(item(-451).affordable)
        assertEquals(1, item(-451).coinsNeeded)
        assertEquals(30, item(-480).coinsNeeded)
    }

    // ---- shop

    @Test
    fun `shop view model redeems records history and refuses to pass the debt limit`() = runTest {
        earn(Tier.Four)
        earn(Tier.Four)
        val cheap = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        val dear = store.createReward("Dinner out", RewardTier.Five, Energy.High, customPrice = 700)
        val vm = ShopViewModel(store)

        assertEquals(80, vm.balance.value)
        assertEquals(listOf(true, false), vm.shelves.value.flatMap { it.items }.map { it.affordable })

        vm.redeem(cheap.id, "r1")
        assertEquals(30, vm.balance.value)
        assertEquals(listOf("Movie night"), store.redemptions.first().map { it.name })
        // 30 coins no longer cover the 50-coin reward, but it can be bought again on credit
        assertEquals(true, vm.shelves.value.flatMap { it.items }.any { it.affordable && it.reward.id == cheap.id })

        vm.redeem(dear.id, "r2")
        assertEquals(30, vm.balance.value)
        assertEquals(1, store.redemptions.first().size)
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
        assertEquals(1, store.redemptions.first().size)
        vm.viewModelScope.cancel()
    }

    // ---- mystery rolls

    private suspend fun earnOn(target: InMemoryStore, tier: Tier) {
        val entry = target.assign(target.create("Earn ${tier.level}", tier, Energy.Low), clock.todayIn(TimeZone.UTC))
        target.completeTask(entry.id)
    }

    @Test
    fun `roll options show price affordability and whether an upgrade is possible`() {
        val rewards = listOf(reward("a", RewardTier.One), reward("b", RewardTier.Two), reward("d", RewardTier.Four))
        val options = buildRollOptions(rewards, balance = -480).associateBy { it.tier }

        assertEquals(listOf(RewardTier.One, RewardTier.Two, RewardTier.Three, RewardTier.Four), options.keys.toList())
        assertEquals(listOf(20, 50, 120, 300), options.values.map { it.price })

        val one = options.getValue(RewardTier.One)
        assertTrue(one.canRoll)
        assertEquals(RewardTier.Two, one.upgradeTier)

        val two = options.getValue(RewardTier.Two)
        assertFalse(two.affordable)
        assertEquals(30, two.coinsNeeded)
        assertFalse(two.canRoll)
        assertEquals(null, two.upgradeTier)

        val three = options.getValue(RewardTier.Three)
        assertFalse(three.hasRewards)
        assertFalse(three.canRoll)
        assertEquals(RewardTier.Four, three.upgradeTier)

        assertEquals(null, options.getValue(RewardTier.Four).upgradeTier)
    }

    @Test
    fun `roll options can borrow up to the debt limit`() {
        val rewards = listOf(reward("b", RewardTier.Two))
        fun two(balance: Int) = buildRollOptions(rewards, balance).first { it.tier == RewardTier.Two }

        assertTrue(two(30).canRoll)
        assertEquals(0, two(30).coinsNeeded)
        assertTrue(two(-450).canRoll)
        assertFalse(two(-451).canRoll)
        assertEquals(1, two(-451).coinsNeeded)
    }

    @Test
    fun `rolling spends the price and shows the result until it is dismissed`() = runTest {
        val rolling = InMemoryStore(clock, random = ScriptedRandom(3, 0))
        earnOn(rolling, Tier.Four)
        earnOn(rolling, Tier.Three)
        rolling.createReward("Movie night", RewardTier.Two, Energy.Low)
        rolling.createReward("Dinner out", RewardTier.Three, Energy.High)
        val vm = ShopViewModel(rolling)

        vm.roll(RewardTier.Two, "roll1")

        val result = vm.lastRoll.value!!
        assertEquals("Movie night", result.redemption.name)
        assertFalse(result.upgraded)
        assertEquals(10, vm.balance.value)
        assertEquals(1, rolling.redemptions.first().size)

        vm.dismissRoll()
        assertEquals(null, vm.lastRoll.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a lucky upgrade is reported in the result`() = runTest {
        val rolling = InMemoryStore(clock, random = ScriptedRandom(0, 0))
        earnOn(rolling, Tier.Four)
        earnOn(rolling, Tier.Three)
        rolling.createReward("Movie night", RewardTier.Two, Energy.Low)
        rolling.createReward("Dinner out", RewardTier.Three, Energy.High)
        val vm = ShopViewModel(rolling)

        vm.roll(RewardTier.Two, "roll1")

        val result = vm.lastRoll.value!!
        assertTrue(result.upgraded)
        assertEquals("Dinner out", result.redemption.name)
        assertEquals(10, vm.balance.value)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a refused roll leaves no result and spends nothing`() = runTest {
        val rolling = InMemoryStore(clock, random = ScriptedRandom())
        earnOn(rolling, Tier.Three)
        rolling.createReward("Movie night", RewardTier.Two, Energy.Low)
        // a 480-coin gift takes the balance to -460, so a 50-coin roll would pass the -500 limit
        val gift = rolling.createReward("Gift", RewardTier.Five, Energy.High, customPrice = 480)
        assertTrue(rolling.redeem(gift.id, "gift"))
        val vm = ShopViewModel(rolling)

        vm.roll(RewardTier.Two, "roll1")

        assertEquals(null, vm.lastRoll.value)
        assertEquals(-460, vm.balance.value)
        assertEquals(listOf("Gift"), rolling.redemptions.first().map { it.name })
        vm.viewModelScope.cancel()
    }

    @Test
    fun `confirming a roll twice with the same id rolls once`() = runTest {
        val rolling = InMemoryStore(clock, random = ScriptedRandom(3, 0))
        repeat(2) { earnOn(rolling, Tier.Four) }
        rolling.createReward("Movie night", RewardTier.Two, Energy.Low)
        val vm = ShopViewModel(rolling)

        vm.roll(RewardTier.Two, "same")
        vm.roll(RewardTier.Two, "same")

        assertEquals(30, vm.balance.value)
        assertEquals(1, rolling.redemptions.first().size)
        vm.viewModelScope.cancel()
    }

    @Test
    fun `a create request stays set until it is consumed`() {
        val vm = RewardTemplatesViewModel(store)
        assertFalse(vm.createRequested.value)
        vm.requestCreate()
        assertTrue(vm.createRequested.value)
        vm.consumeCreateRequest()
        assertFalse(vm.createRequested.value)
        vm.viewModelScope.cancel()
    }
}
