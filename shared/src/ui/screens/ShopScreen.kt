package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.formatCoins
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.GameRules
import com.usuallycoding.openadhd.ui.LocalGameRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollResult
import com.usuallycoding.openadhd.domain.RollRules
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.ui.components.DayNavigator
import com.usuallycoding.openadhd.ui.shop.RewardHistoryDay
import com.usuallycoding.openadhd.ui.shop.RewardHistoryViewModel
import com.usuallycoding.openadhd.ui.shop.RollOption
import com.usuallycoding.openadhd.ui.shop.ShopItem
import com.usuallycoding.openadhd.ui.shop.ShopShelf
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The Shop destination: rewards to buy and mystery rolls, and the history of purchases. */
@Composable
fun ShopScreen(
    viewModel: ShopViewModel,
    historyViewModel: RewardHistoryViewModel,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { 2 },
) {
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val date by historyViewModel.date.collectAsStateWithLifecycle()
    val today by historyViewModel.today.collectAsStateWithLifecycle()
    val canGoNext by historyViewModel.canGoNext.collectAsStateWithLifecycle()
    val redemptions by historyViewModel.redemptions.collectAsStateWithLifecycle()
    val days by historyViewModel.daysWithRedemptions.collectAsStateWithLifecycle()
    val rollOptions by viewModel.rollOptions.collectAsStateWithLifecycle()
    val lastRoll by viewModel.lastRoll.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    ShopContent(
        shelves = shelves,
        history = RewardHistoryDay(date, today, canGoNext, redemptions, days),
        rollOptions = rollOptions,
        lastRoll = lastRoll,
        onRedeem = viewModel::redeem,
        onRoll = viewModel::roll,
        onDismissRoll = viewModel::dismissRoll,
        modifier = modifier,
        pagerState = pagerState,
        balance = balance,
        onPreviousDay = historyViewModel::previousDay,
        onNextDay = historyViewModel::nextDay,
        onPickDay = historyViewModel::pickDay,
    )
}

@Composable
fun ShopContent(
    shelves: List<ShopShelf>,
    history: RewardHistoryDay,
    rollOptions: List<RollOption>,
    lastRoll: RollResult?,
    onRedeem: (rewardId: String, redemptionId: String) -> Unit,
    onRoll: (tier: RewardTier, rollId: String) -> Unit,
    onDismissRoll: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { 2 },
    balance: Int = 0,
    onPreviousDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onPickDay: (LocalDate) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    Column(modifier) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Rewards") },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("History") },
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) ShelvesList(shelves, rollOptions, balance, onRedeem, onRoll) else HistoryList(history, onPreviousDay, onNextDay, onPickDay)
        }
    }

    lastRoll?.let { RollRevealDialog(it, onDismissRoll) }
}

@Composable
private fun ShelvesList(
    shelves: List<ShopShelf>,
    rollOptions: List<RollOption>,
    balance: Int,
    onRedeem: (String, String) -> Unit,
    onRoll: (RewardTier, String) -> Unit,
) {
    var confirming by remember { mutableStateOf<RewardTemplate?>(null) }
    var rolling by remember { mutableStateOf<RollOption?>(null) }

    if (shelves.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "The shop is empty. Add rewards on the Templates screen, in the Rewards tab.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "rolls") { SectionTitle("Mystery roll") }
            items(rollOptions, key = { "roll-${it.tier.level}" }) { option ->
                RollRow(option, onRoll = { rolling = option })
                HorizontalDivider()
            }
            shelves.forEach { shelf ->
                item(key = "tier-${shelf.tier.level}") { SectionTitle(shelf.title(LocalGameRules.current)) }
                items(shelf.items, key = { it.reward.id }) { item ->
                    ShopRow(item, onRedeem = { confirming = item.reward })
                    HorizontalDivider()
                }
            }
        }
    }

    confirming?.let { reward ->
        ConfirmRedeemDialog(
            reward = reward,
            balance = balance,
            onConfirm = { redemptionId ->
                onRedeem(reward.id, redemptionId)
                confirming = null
            },
            onDismiss = { confirming = null },
        )
    }
    rolling?.let { option ->
        ConfirmRollDialog(
            option = option,
            balance = balance,
            onConfirm = { rollId ->
                onRoll(option.tier, rollId)
                rolling = null
            },
            onDismiss = { rolling = null },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ShopRow(item: ShopItem, onRedeem: () -> Unit) {
    val reward = item.reward
    ListItem(
        headlineContent = { Text(reward.name) },
        supportingContent = {
            Column {
                if (reward.description.isNotBlank()) Text(reward.description)
                Text("${formatCoins(reward.price)} coins · ${reward.energy.name} energy")
                if (!item.affordable) {
                    Text(
                        "${formatCoins(item.coinsNeeded)} more coins needed",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        trailingContent = {
            Button(
                onClick = onRedeem,
                enabled = item.affordable,
                modifier = Modifier.semantics { contentDescription = "Redeem ${reward.name}" },
            ) { Text("Redeem") }
        },
    )
}

@Composable
private fun RollRow(option: RollOption, onRoll: () -> Unit) {
    ListItem(
        headlineContent = { Text("Tier ${option.tier.level} roll · ${formatCoins(option.price)} coins") },
        supportingContent = {
            when {
                !option.hasRewards -> Text("No tier ${option.tier.level} rewards yet")
                !option.affordable -> Text(
                    "${formatCoins(option.coinsNeeded)} more coins needed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> Text(option.oddsText(LocalGameRules.current.upgradeOneIn))
            }
        },
        trailingContent = {
            Button(
                onClick = onRoll,
                enabled = option.canRoll,
                modifier = Modifier.semantics { contentDescription = "Roll tier ${option.tier.level}" },
            ) { Text("Roll") }
        },
    )
}

/** What a roll gives, in words; mentions the upgrade chance only when an upgrade is possible. */
private fun RollOption.oddsText(upgradeOneIn: Int): String {
    val base = "A random tier ${tier.level} reward"
    val upgrade = upgradeTier ?: return base
    return "$base, with a 1 in $upgradeOneIn chance of a tier ${upgrade.level} one"
}

/** The extra confirmation line when a purchase leaves the user in debt; nothing when it does not. */
private fun debtNote(debt: Int): String =
    if (debt <= 0) "" else " This puts you ${if (debt == 1) "1 coin" else "${formatCoins(debt)} coins"} in debt."

@Composable
private fun ConfirmRedeemDialog(
    reward: RewardTemplate,
    balance: Int,
    onConfirm: (redemptionId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // One id per opening of the dialog: confirming twice in a row can only ever record one purchase.
    val redemptionId = remember { newId() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redeem ${reward.name}?") },
        text = { Text("This costs ${formatCoins(reward.price)} coins.${debtNote(CoinRules.debtAfter(balance, reward.price))}") },
        confirmButton = { TextButton(onClick = { onConfirm(redemptionId) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmRollDialog(
    option: RollOption,
    balance: Int,
    onConfirm: (rollId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // One id per opening of the dialog: confirming twice in a row can only ever roll once.
    val rollId = remember { newId() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Roll tier ${option.tier.level}?") },
        text = { Text("This costs ${formatCoins(option.price)} coins. ${option.oddsText(LocalGameRules.current.upgradeOneIn)}.${debtNote(CoinRules.debtAfter(balance, option.price))}") },
        confirmButton = { TextButton(onClick = { onConfirm(rollId) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RollRevealDialog(result: RollResult, onDismiss: () -> Unit) {
    val won = result.redemption
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("You got: ${won.name}!") },
        text = {
            Column {
                val rolled = won.rolledTier
                if (result.upgraded && rolled != null) {
                    Text(
                        "Lucky upgrade! You rolled tier ${rolled.level} and won a tier ${won.tier.level} reward.",
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text("A tier ${won.tier.level} reward.")
                }
                Text("It cost ${formatCoins(won.price)} coins.", modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun HistoryList(
    history: RewardHistoryDay,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDay: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // Today is allowed here, unlike on the Today screen's History, because a reward can be redeemed today.
        DayNavigator(
            date = history.date,
            today = history.today,
            canGoNext = history.canGoNext,
            daysWithEntries = history.daysWithRedemptions,
            includeToday = true,
            markedLabel = "redemptions",
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onPickDay = onPickDay,
        )
        HorizontalDivider()
        if (history.redemptions.isEmpty()) {
            Box(Modifier.fillMaxSize()) {
                Text(
                    "Nothing redeemed on this day",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
        } else {
            LazyColumn {
                items(history.redemptions, key = { it.id }) { purchase ->
                    ListItem(
                        headlineContent = { Text(purchase.name) },
                        supportingContent = {
                            Column {
                                if (purchase.description.isNotBlank()) Text(purchase.description)
                                Text(purchase.historyNote())
                            }
                        },
                        trailingContent = { Text("-${formatCoins(purchase.price)}") },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** "Tier 2 · when" for a purchase; rolls say what was rolled and whether it was a lucky upgrade. */
private fun Redemption.historyNote(): String {
    val rolled = rolledTier
    if (source != RedemptionSource.Roll || rolled == null) return "Tier ${tier.level} · ${formatTime(redeemedAt)}"
    val upgrade = if (isLuckyUpgrade) " · Lucky upgrade (tier ${tier.level} reward)" else ""
    return "Mystery roll of tier ${rolled.level}$upgrade · ${formatTime(redeemedAt)}"
}

private fun ShopShelf.title(rules: GameRules): String =
    if (tier == RewardTier.Five) "Tier 5 · your own price" else "Tier ${tier.level} · ${RewardRules.fixedPrice(tier, rules)?.let(::formatCoins)} coins"

private fun formatTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(value: Int) = value.toString().padStart(2, '0')
    return "${local.date} ${two(local.hour)}:${two(local.minute)}"
}

@Preview
@Composable
private fun ShopContentPreview() {
    val movie = RewardTemplate("1", "Movie night", RewardTier.Two, Energy.Low, "With snacks")
    val headphones = RewardTemplate("2", "New headphones", RewardTier.Five, Energy.Low, customPrice = 800)
    MaterialTheme {
        ShopContent(
            shelves = listOf(
                ShopShelf(RewardTier.Two, listOf(ShopItem(movie, affordable = true, coinsNeeded = 0))),
                ShopShelf(RewardTier.Five, listOf(ShopItem(headphones, affordable = false, coinsNeeded = 750))),
            ),
            history = RewardHistoryDay(LocalDate(2026, 10, 24), LocalDate(2026, 10, 24), false, emptyList(), emptySet()),
            rollOptions = listOf(
                RollOption(RewardTier.One, 20, affordable = true, coinsNeeded = 0, hasRewards = false, upgradeTier = RewardTier.Two),
                RollOption(RewardTier.Two, 50, affordable = true, coinsNeeded = 0, hasRewards = true, upgradeTier = null),
            ),
            lastRoll = null,
            onRedeem = { _, _ -> },
            onRoll = { _, _ -> },
            onDismissRoll = {},
        )
    }
}
