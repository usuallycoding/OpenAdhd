package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.ui.shop.ShopItem
import com.usuallycoding.openadhd.ui.shop.ShopShelf
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The Shop destination: rewards to buy, and the history of purchases. */
@Composable
fun ShopScreen(viewModel: ShopViewModel, modifier: Modifier = Modifier) {
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    ShopContent(shelves, history, onRedeem = viewModel::redeem, modifier = modifier)
}

@Composable
fun ShopContent(
    shelves: List<ShopShelf>,
    history: List<Redemption>,
    onRedeem: (rewardId: String, redemptionId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(modifier) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Rewards") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
        }
        if (tab == 0) ShelvesList(shelves, onRedeem) else HistoryList(history)
    }
}

@Composable
private fun ShelvesList(shelves: List<ShopShelf>, onRedeem: (String, String) -> Unit) {
    var confirming by remember { mutableStateOf<RewardTemplate?>(null) }

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
            shelves.forEach { shelf ->
                item(key = "tier-${shelf.tier.level}") {
                    Text(
                        shelf.title(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                    )
                }
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
                    supportingContent = { Text("Tier ${purchase.tier.level} · ${formatTime(purchase.redeemedAt)}") },
                    trailingContent = { Text("-${purchase.price}") },
                )
                HorizontalDivider()
            }
        }
    }
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
            onRedeem = { _, _ -> },
        )
    }
}
