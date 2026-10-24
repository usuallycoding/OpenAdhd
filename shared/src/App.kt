package com.usuallycoding.openadhd

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.ui.components.ThemeChooser
import com.usuallycoding.openadhd.ui.screens.ShopScreen
import com.usuallycoding.openadhd.ui.screens.TemplatesScreen
import com.usuallycoding.openadhd.ui.screens.TodayScreen
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.theme.AppTheme
import com.usuallycoding.openadhd.ui.theme.CoinIcon
import com.usuallycoding.openadhd.ui.theme.ReorderIcon
import com.usuallycoding.openadhd.ui.theme.ShopIcon
import com.usuallycoding.openadhd.ui.theme.TemplatesIcon
import com.usuallycoding.openadhd.ui.theme.ThemeMode
import com.usuallycoding.openadhd.ui.theme.TodayIcon
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import com.usuallycoding.openadhd.ui.wallet.WalletViewModel
import org.jetbrains.compose.reload.DevelopmentEntryPoint

private enum class Destination(val label: String, val icon: ImageVector) {
    Today("Today", TodayIcon),
    Templates("Templates", TemplatesIcon),
    Shop("Shop", ShopIcon),
}

/** Previews and hot reload use this in-memory container; each platform entry point passes a persistent one. */
@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    App(remember { AppContainer(InMemoryStore()) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(container: AppContainer) {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
    var destination by rememberSaveable { mutableStateOf(Destination.Today) }

    val todayViewModel = viewModel { TodayViewModel(container.store, container.clock, dateCheckInterval = container.dateCheckInterval, random = container.random) }
    val templatesViewModel = viewModel { TemplatesViewModel(container.store) }
    val rewardTemplatesViewModel = viewModel { RewardTemplatesViewModel(container.store) }
    val shopViewModel = viewModel { ShopViewModel(container.store) }
    val walletViewModel = viewModel { WalletViewModel(container.store) }
    val balance by walletViewModel.balance.collectAsStateWithLifecycle()

    // App never leaves composition, so these keep each screen's selected tab while the app runs.
    val templatesPager = rememberPagerState { 2 }
    val shopPager = rememberPagerState { 2 }

    // The Reorder toggle in the top bar belongs to whichever list is on screen: Today, or the shown Templates tab.
    val todayReordering by todayViewModel.reordering.collectAsStateWithLifecycle()
    val todayCanReorder by todayViewModel.canReorder.collectAsStateWithLifecycle()
    val templatesReordering by templatesViewModel.reordering.collectAsStateWithLifecycle()
    val templatesCanReorder by templatesViewModel.canReorder.collectAsStateWithLifecycle()
    val rewardsReordering by rewardTemplatesViewModel.reordering.collectAsStateWithLifecycle()
    val rewardsCanReorder by rewardTemplatesViewModel.canReorder.collectAsStateWithLifecycle()
    val reorderToggle: ReorderToggle? = when (destination) {
        Destination.Today -> ReorderToggle(todayReordering, todayCanReorder, todayViewModel::setReordering)
        Destination.Templates ->
            if (templatesPager.currentPage == 0) {
                ReorderToggle(templatesReordering, templatesCanReorder, templatesViewModel::setReordering)
            } else {
                ReorderToggle(rewardsReordering, rewardsCanReorder, rewardTemplatesViewModel::setReordering)
            }
        Destination.Shop -> null
    }
    // Leaving a list ends its Reorder mode, so it is never left on out of sight.
    LaunchedEffect(destination, templatesPager.currentPage) {
        todayViewModel.setReordering(false)
        templatesViewModel.setReordering(false)
        rewardTemplatesViewModel.setReordering(false)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(todayViewModel) {
        // Each critical hit gets a short message; showSnackbar waits its turn, so quick hits queue up instead of overlapping.
        todayViewModel.criticalHits.collect { hit ->
            snackbarHostState.showSnackbar("Critical hit! +${hit.bonus} bonus coins")
        }
    }

    AppTheme(themeMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("OpenAdhd") },
                    actions = {
                        if (reorderToggle != null && (reorderToggle.canReorder || reorderToggle.on)) {
                            IconToggleButton(checked = reorderToggle.on, onCheckedChange = reorderToggle.onChange) {
                                Icon(ReorderIcon, contentDescription = if (reorderToggle.on) "Finish reordering" else "Reorder")
                            }
                        }
                        BalanceChip(balance)
                        ThemeChooser(
                            selected = themeMode,
                            onSelected = { themeMode = it },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            },
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == destination,
                            onClick = { destination = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            val screenModifier = Modifier.padding(innerPadding)
            when (destination) {
                Destination.Today -> TodayScreen(todayViewModel, screenModifier)
                Destination.Templates -> TemplatesScreen(templatesViewModel, rewardTemplatesViewModel, screenModifier, templatesPager)
                Destination.Shop -> ShopScreen(shopViewModel, screenModifier, shopPager)
            }
        }
    }
}

private class ReorderToggle(val on: Boolean, val canReorder: Boolean, val onChange: (Boolean) -> Unit)

@Composable
private fun BalanceChip(balance: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(CoinIcon, contentDescription = "Dopamine coins")
        Text("$balance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
    }
}
