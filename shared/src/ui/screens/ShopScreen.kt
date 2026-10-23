package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollResult
import com.usuallycoding.openadhd.domain.RollRules
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.ui.shop.RollOption
import com.usuallycoding.openadhd.ui.shop.ShopItem
import com.usuallycoding.openadhd.ui.shop.ShopShelf
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The Shop destination: rewards to buy and mystery rolls, and the history of purchases. */
@Composable
fun ShopScreen(viewModel: ShopViewModel, modifier: Modifier = Modifier) {
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val rollOptions by viewModel.rollOptions.collectAsStateWithLifecycle()
    val lastRoll by viewModel.lastRoll.collectAsStateWithLifecycle()
    ShopContent(
        shelves = shelves,
        history = history,
        rollOptions = rollOptions,
        lastRoll = lastRoll,
        onRedeem = viewModel::redeem,
        onRoll = viewModel::roll,
        onDismissRoll = viewModel::dismissRoll,
        modifier = modifier,
    )
}

@Composable
fun ShopContent(
    shelves: List<ShopShelf>,
    history: List<Redemption>,
    rollOptions: List<RollOption>,
    lastRoll: RollResult?,
    onRedeem: (rewardId: String, redemptionId: String) -> Unit,
    onRoll: (tier: RewardTier, rollId: String) -> Unit,
    onDismissRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(modifier) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Rewards") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
        }
        if (tab == 0) ShelvesList(shelves, rollOptions, onRedeem, onRoll) else HistoryList(history)
    }

    lastRoll?.let { RollRevealDialog(it, onDismissRoll) }
}

@Composable
private fun ShelvesList(
    shelves: List<ShopShelf>,
    rollOptions: List<RollOption>,
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
                item(key = "tier-${shelf.tier.level}") { SectionTitle(shelf.title()) }
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
                Text("${reward.price} coins · ${reward.energy.name} energy")
                if (!item.affordable) {
                    Text(
                        "${item.coinsNeeded} more coins needed",
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
        headlineContent = { Text("Tier ${option.tier.level} roll · ${option.price} coins") },
        supportingContent = {
            when {
                !option.hasRewards -> Text("No tier ${option.tier.level} rewards yet")
                !option.affordable -> Text(
                    "${option.coinsNeeded} more coins needed",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> Text(option.oddsText())
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
private fun RollOption.oddsText(): String {
    val base = "A random tier ${tier.level} reward"
    val upgrade = upgradeTier ?: return base
    return "$base, with a 1 in ${RollRules.UpgradeOneIn} chance of a tier ${upgrade.level} one"
}

@Composable
private fun ConfirmRedeemDialog(reward: RewardTemplate, onConfirm: (redemptionId: String) -> Unit, onDismiss: () -> Unit) {
    // One id per opening of the dialog: confirming twice in a row can only ever record one purchase.
    val redemptionId = remember { newId() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redeem ${reward.name}?") },
        text = { Text("This costs ${reward.price} coins.") },
        confirmButton = { TextButton(onClick = { onConfirm(redemptionId) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ConfirmRollDialog(option: RollOption, onConfirm: (rollId: String) -> Unit, onDismiss: () -> Unit) {
    // One id per opening of the dialog: confirming twice in a row can only ever roll once.
    val rollId = remember { newId() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Roll tier ${option.tier.level}?") },
        text = { Text("This costs ${option.price} coins. ${option.oddsText()}.") },
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
                Text("It cost ${won.price} coins.", modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun HistoryList(history: List<Redemption>) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "Nothing redeemed yet.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
    } else {
        LazyColumn {
            items(history, key = { it.id }) { purchase ->
                ListItem(
                    headlineContent = { Text(purchase.name) },
                    supportingContent = { Text(purchase.historyNote()) },
                    trailingContent = { Text("-${purchase.price}") },
                )
                HorizontalDivider()
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

private fun ShopShelf.title(): String =
    if (tier == RewardTier.Five) "Tier 5 · your own price" else "Tier ${tier.level} · ${RewardRules.fixedPrice(tier)} coins"

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
            history = emptyList(),
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
