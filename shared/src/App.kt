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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.usuallycoding.openadhd.domain.formatCoins
import com.usuallycoding.openadhd.data.AppContainer
import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.ReminderCoordinator
import com.usuallycoding.openadhd.domain.ScheduledEntries
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.ui.LocalGameRules
import com.usuallycoding.openadhd.ui.screens.SettingsScreen
import com.usuallycoding.openadhd.ui.screens.ShopScreen
import com.usuallycoding.openadhd.ui.screens.TemplatesScreen
import com.usuallycoding.openadhd.ui.screens.TodayScreen
import com.usuallycoding.openadhd.ui.rewards.RewardTemplatesViewModel
import com.usuallycoding.openadhd.ui.settings.SettingsViewModel
import com.usuallycoding.openadhd.ui.shop.RewardHistoryViewModel
import com.usuallycoding.openadhd.ui.shop.ShopViewModel
import com.usuallycoding.openadhd.ui.templates.TemplatesViewModel
import com.usuallycoding.openadhd.ui.theme.AppTheme
import com.usuallycoding.openadhd.ui.theme.CoinIcon
import com.usuallycoding.openadhd.ui.theme.ReorderIcon
import com.usuallycoding.openadhd.ui.theme.SettingsIcon
import com.usuallycoding.openadhd.ui.theme.ShopIcon
import com.usuallycoding.openadhd.ui.theme.TemplatesIcon
import com.usuallycoding.openadhd.ui.theme.TodayIcon
import com.usuallycoding.openadhd.ui.history.HistoryViewModel
import com.usuallycoding.openadhd.ui.today.TodayViewModel
import com.usuallycoding.openadhd.ui.wallet.WalletViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.DevelopmentEntryPoint

private enum class Destination(val label: String, val icon: ImageVector) {
    Today("Today", TodayIcon),
    Templates("Templates", TemplatesIcon),
    Shop("Shop", ShopIcon),
    Settings("Settings", SettingsIcon),
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
    var destination by rememberSaveable { mutableStateOf(Destination.Today) }

    val todayViewModel = viewModel { TodayViewModel(container.store, container.clock, dateCheckInterval = container.dateCheckInterval, random = container.random, reminders = container.reminders) }
    val templatesViewModel = viewModel { TemplatesViewModel(container.store, container.store.settings, container.reminders) }
    val rewardTemplatesViewModel = viewModel { RewardTemplatesViewModel(container.store, container.store.settings) }
    val historyViewModel = viewModel { HistoryViewModel(container.store, todayViewModel.today) }
    val shopViewModel = viewModel { ShopViewModel(container.store) }
    val rewardHistoryViewModel = viewModel { RewardHistoryViewModel(container.store, todayViewModel.today) }
    val walletViewModel = viewModel { WalletViewModel(container.store) }
    val settingsViewModel = viewModel { SettingsViewModel(container.store, container.files, container.clock, loginItem = container.loginItem, versionLabel = container.versionLabel) }
    val balance by walletViewModel.balance.collectAsStateWithLifecycle()
    val settings by container.store.settings.collectAsStateWithLifecycle(container.initialSettings)

    // Adds the entries of scheduled templates to Today when the app starts, when the date changes and when a template changes.
    LaunchedEffect(container) {
        ScheduledEntries(container.store, todayViewModel.today).run()
    }

    // Keeps the platform's notifications equal to the reminders in the store, for as long as the app is shown.
    LaunchedEffect(container) {
        val scheduler = container.reminders ?: return@LaunchedEffect
        ReminderCoordinator(container.store, scheduler, container.clock, todayViewModel.today).run()
    }

    // App never leaves composition, so these keep each screen's selected tab while the app runs.
    val todayPager = rememberPagerState { 2 }
    val templatesPager = rememberPagerState { 2 }
    val shopPager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    // A tapped reminder notification asks for its entry: show Today with that entry scrolled to and highlighted.
    LaunchedEffect(container) {
        container.entryRequests.pending.collect { entryId ->
            if (entryId == null) return@collect
            destination = Destination.Today
            todayViewModel.setReordering(false)
            todayPager.scrollToPage(0)
            todayViewModel.reveal(entryId)
            container.entryRequests.consume(entryId)
        }
    }

    // The Reorder toggle in the top bar belongs to whichever list is on screen: Today (not its History tab), or the shown Templates tab.
    val todayReordering by todayViewModel.reordering.collectAsStateWithLifecycle()
    val todayCanReorder by todayViewModel.canReorder.collectAsStateWithLifecycle()
    val templatesReordering by templatesViewModel.reordering.collectAsStateWithLifecycle()
    val templatesCanReorder by templatesViewModel.canReorder.collectAsStateWithLifecycle()
    val rewardsReordering by rewardTemplatesViewModel.reordering.collectAsStateWithLifecycle()
    val rewardsCanReorder by rewardTemplatesViewModel.canReorder.collectAsStateWithLifecycle()
    val reorderToggle: ReorderToggle? = when (destination) {
        Destination.Today ->
            if (todayPager.currentPage == 0) ReorderToggle(todayReordering, todayCanReorder, todayViewModel::setReordering) else null
        Destination.Templates ->
            if (templatesPager.currentPage == 0) {
                ReorderToggle(templatesReordering, templatesCanReorder, templatesViewModel::setReordering)
            } else {
                ReorderToggle(rewardsReordering, rewardsCanReorder, rewardTemplatesViewModel::setReordering)
            }
        Destination.Shop, Destination.Settings -> null
    }
    // Leaving a list ends its Reorder mode, so it is never left on out of sight.
    LaunchedEffect(destination, todayPager.currentPage, templatesPager.currentPage) {
        todayViewModel.setReordering(false)
        templatesViewModel.setReordering(false)
        rewardTemplatesViewModel.setReordering(false)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(todayViewModel) {
        // Each critical hit gets a short message; showSnackbar waits its turn, so quick hits queue up instead of overlapping.
        todayViewModel.criticalHits.collect { hit ->
            snackbarHostState.showSnackbar("Critical hit! +${formatCoins(hit.bonus)} bonus coins")
        }
    }

    // Where the platform cannot show a system notification (the desktop without a system tray), reminders show here.
    LaunchedEffect(container) {
        container.reminderNotices.collect { text -> snackbarHostState.showSnackbar("Still to do: $text") }
    }

    LaunchedEffect(templatesViewModel) {
        templatesViewModel.notices.collect { text -> snackbarHostState.showSnackbar(text) }
    }

    LaunchedEffect(todayViewModel) {
        todayViewModel.undone.collect { undone ->
            val coins = if (undone.coins == 1) "1 coin" else "${formatCoins(undone.coins)} coins"
            snackbarHostState.showSnackbar(if (undone.coins == 0) "Undone" else "Undone, -$coins")
        }
    }

    CompositionLocalProvider(LocalGameRules provides settings.rules) {
    AppTheme(settings.themeMode, settings.dynamicColor) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(settings.title) },
                    actions = {
                        if (reorderToggle != null && (reorderToggle.canReorder || reorderToggle.on)) {
                            IconToggleButton(checked = reorderToggle.on, onCheckedChange = reorderToggle.onChange) {
                                Icon(ReorderIcon, contentDescription = if (reorderToggle.on) "Finish reordering" else "Reorder")
                            }
                        }
                        BalanceChip(balance)
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
                Destination.Today -> TodayScreen(
                    todayViewModel, historyViewModel, screenModifier, todayPager,
                    onTakeMeToTemplates = {
                        destination = Destination.Templates
                        scope.launch { templatesPager.scrollToPage(0) }
                        templatesViewModel.requestCreate()
                    },
                )
                Destination.Templates -> TemplatesScreen(templatesViewModel, rewardTemplatesViewModel, screenModifier, templatesPager)
                Destination.Shop -> ShopScreen(
                    shopViewModel, rewardHistoryViewModel, screenModifier, shopPager,
                    onTakeMeToRewards = {
                        destination = Destination.Templates
                        scope.launch { templatesPager.scrollToPage(1) }
                        rewardTemplatesViewModel.requestCreate()
                    },
                )
                Destination.Settings -> SettingsScreen(settingsViewModel, screenModifier)
            }
        }
    }
    }
}

private class ReorderToggle(val on: Boolean, val canReorder: Boolean, val onChange: (Boolean) -> Unit)

@Composable
private fun BalanceChip(balance: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Icon(CoinIcon, contentDescription = "Dopamine coins")
        // A balance below zero is a debt, so it is shown in the error colour.
        Text(
            formatCoins(balance),
            style = MaterialTheme.typography.titleMedium,
            color = if (balance < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
