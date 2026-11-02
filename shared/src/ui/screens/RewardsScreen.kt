package com.usuallycoding.openadhd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usuallycoding.openadhd.domain.formatCoins
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.ui.LocalGameRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.components.FormSheet
import com.usuallycoding.openadhd.ui.components.MoveButtons
import com.usuallycoding.openadhd.ui.components.SearchField
import com.usuallycoding.openadhd.ui.components.dismissKeyboardOnTapOrScroll
import com.usuallycoding.openadhd.ui.theme.AddIcon
import com.usuallycoding.openadhd.ui.theme.DeleteIcon
import com.usuallycoding.openadhd.ui.theme.EditIcon

@Composable
fun RewardsScreen(viewModel: RewardTemplatesViewModel, modifier: Modifier = Modifier) {
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    val visible by viewModel.visible.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val reordering by viewModel.reordering.collectAsStateWithLifecycle()
    val createRequested by viewModel.createRequested.collectAsStateWithLifecycle()
    RewardsContent(
        rewards = visible,
        totalCount = rewards.size,
        query = query,
        onQueryChange = viewModel::setQuery,
        reordering = reordering,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        modifier = modifier,
        createRequested = createRequested,
        onCreateRequestConsumed = viewModel::consumeCreateRequest,
    )
}

@Composable
fun RewardsContent(
    rewards: List<RewardTemplate>,
    onSave: (id: String?, name: String, tier: RewardTier, energy: Energy, description: String, customPrice: Int?) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    /** How many rewards exist, so a search that matches none can be told apart from having none. */
    totalCount: Int = rewards.size,
    /** The text of the search above the list, and what to do when it changes. */
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    reordering: Boolean = false,
    onMoveUp: (id: String) -> Unit = {},
    onMoveDown: (id: String) -> Unit = {},
    /** Another screen asked for the create form: it opens once and [onCreateRequestConsumed] clears the request. */
    createRequested: Boolean = false,
    onCreateRequestConsumed: () -> Unit = {},
) {
    // null = closed; a reward with a blank id = creating; otherwise editing that reward
    var editing by remember { mutableStateOf<RewardTemplate?>(null) }

    LaunchedEffect(createRequested) {
        if (createRequested) {
            editing = RewardTemplate("", "", RewardTier.One, Energy.Low)
            onCreateRequestConsumed()
        }
    }

    Box(modifier.fillMaxSize()) {
        if (totalCount == 0) {
            Text(
                "No rewards yet. Tap + to add something to look forward to.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        } else Column(Modifier.fillMaxSize()) {
            SearchField(query, onQueryChange, label = "Search rewards", clearDescription = "Clear reward search")
            Box(Modifier.weight(1f).fillMaxWidth().dismissKeyboardOnTapOrScroll()) {
                if (rewards.isEmpty()) {
                    Text(
                        "No reward matches ${query.trim()}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                } else LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    itemsIndexed(rewards, key = { _, reward -> reward.id }) { index, reward ->
                        ListItem(
                            headlineContent = { Text(reward.name) },
                            supportingContent = {
                                Column {
                                    if (reward.description.isNotBlank()) Text(reward.description)
                                    Text(reward.summary())
                                }
                            },
                            trailingContent = {
                                if (reordering) {
                                    MoveButtons(
                                        name = reward.name,
                                        canMoveUp = index > 0,
                                        canMoveDown = index < rewards.lastIndex,
                                        onMoveUp = { onMoveUp(reward.id) },
                                        onMoveDown = { onMoveDown(reward.id) },
                                    )
                                } else {
                                    Row {
                                        IconButton(onClick = { editing = reward }) {
                                            Icon(EditIcon, contentDescription = "Edit ${reward.name}")
                                        }
                                        IconButton(onClick = { onDelete(reward.id) }) {
                                            Icon(DeleteIcon, contentDescription = "Delete ${reward.name}")
                                        }
                                    }
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        if (!reordering) {
            FloatingActionButton(
                onClick = { editing = RewardTemplate("", "", RewardTier.One, Energy.Low) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(AddIcon, contentDescription = "Add reward")
            }
        }
    }

    editing?.let { reward ->
        RewardDialog(
            initial = reward,
            onDismiss = { editing = null },
            onConfirm = { name, tier, energy, description, customPrice ->
                onSave(reward.id.ifEmpty { null }, name, tier, energy, description, customPrice)
            },
        )
    }
}

@Composable
private fun RewardDialog(
    initial: RewardTemplate,
    onDismiss: () -> Unit,
    onConfirm: (name: String, tier: RewardTier, energy: Energy, description: String, customPrice: Int?) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var tier by remember { mutableStateOf(initial.tier) }
    var energy by remember { mutableStateOf(initial.energy) }
    var priceText by remember { mutableStateOf(initial.customPrice?.toString() ?: "") }
    var error by remember { mutableStateOf<RewardInputError?>(null) }
    val rules = LocalGameRules.current

    FormSheet(
        title = if (initial.id.isEmpty()) "New reward" else "Edit reward",
        onDismiss = onDismiss,
        footer = { requestClose ->
            TextButton(onClick = requestClose) { Text("Cancel") }
            TextButton(onClick = {
                val customPrice = if (tier == RewardTier.Five) priceText.toIntOrNull() else null
                val problem = RewardTemplatesViewModel.validate(name, tier, customPrice, rules)
                if (problem == null) {
                    onConfirm(name, tier, energy, description, customPrice)
                    requestClose()
                } else {
                    error = problem
                }
            }) { Text("Save") }
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text("Name") },
            singleLine = true,
            isError = error == RewardInputError.NameRequired,
            supportingText = if (error == RewardInputError.NameRequired) ({ Text("A name is required") }) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Tier", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardTier.entries.forEach { option ->
                FilterChip(
                    selected = option == tier,
                    onClick = { tier = option; error = null },
                    label = { Text("${option.level}") },
                )
            }
        }
        if (tier == RewardTier.Five) {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it.filter(Char::isDigit); error = null },
                label = { Text("Price (coins)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = error == RewardInputError.PriceRequired || error == RewardInputError.PriceTooLow,
                supportingText = {
                    Text(
                        when (error) {
                            RewardInputError.PriceRequired -> "A price is required for tier 5"
                            RewardInputError.PriceTooLow ->
                                "A tier 5 price must be larger than ${RewardRules.minCustomPrice(rules) - 1}"
                            else -> "Must be larger than ${RewardRules.minCustomPrice(rules) - 1}"
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text("Costs ${formatCoins(RewardRules.price(tier, null, rules))} coins", style = MaterialTheme.typography.bodyMedium)
        }
        Text("Energy it takes", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Energy.entries.forEach { option ->
                FilterChip(
                    selected = option == energy,
                    onClick = { energy = option },
                    label = { Text(option.name) },
                )
            }
        }
    }
}

internal fun RewardTemplate.summary() = "Tier ${tier.level} · ${energy.name} energy · ${formatCoins(price)} coins"

@Preview
@Composable
private fun RewardsContentPreview() {
    MaterialTheme {
        RewardsContent(
            rewards = listOf(
                RewardTemplate("1", "Movie night", RewardTier.Two, Energy.Low, "With snacks"),
                RewardTemplate("2", "New headphones", RewardTier.Five, Energy.Low, customPrice = 800),
            ),
            onSave = { _, _, _, _, _, _ -> },
            onDelete = {},
        )
    }
}
