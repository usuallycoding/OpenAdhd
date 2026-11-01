package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.EntryRequests
import com.usuallycoding.openadhd.domain.LoginItem
import com.usuallycoding.openadhd.domain.ReminderScheduler
import com.usuallycoding.openadhd.ui.today.DefaultDateCheckInterval
import com.usuallycoding.openadhd.domain.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration

/** The app's long-lived dependencies, created once per platform entry point. */
class AppContainer(
    val store: OpenAdhdStore,
    val clock: Clock = Clock.System,
    val dateCheckInterval: Duration? = DefaultDateCheckInterval,
    val random: Random = Random.Default,
    /** Saving and opening backup files, or null where the platform has no file dialog. */
    val files: DataFiles? = null,
    /** Delivering reminders, or null where the platform has no way to (previews and tests). */
    val reminders: ReminderScheduler? = null,
    /** Reminders to show inside the app, where the platform has no system notification to show them with (the desktop fallback). */
    val reminderNotices: SharedFlow<String> = MutableSharedFlow(),
    /**
     * The saved settings, read once while the container is built, before any screen is drawn. The first frame
     * then already has the saved theme and name instead of the defaults.
     */
    val initialSettings: Settings = runBlocking { store.settings.first() },
    /** Where a platform asks the app to show an entry, such as the one a tapped reminder notification is about. */
    val entryRequests: EntryRequests = EntryRequests(),
    /** Starting the app at login, or null where the platform has no such thing (everywhere but the packaged macOS app). */
    val loginItem: LoginItem? = null,
)

/**
 * The real app's wiring, shared by every platform entry point: a Room-backed store with critical hits switched on.
 * Stores default to no critical hits, so this is the one place that turns them on for the running app.
 */
fun createContainer(
    database: OpenAdhdDatabase,
    files: DataFiles? = null,
    reminders: ReminderScheduler? = null,
    reminderNotices: SharedFlow<String> = MutableSharedFlow(),
    entryRequests: EntryRequests = EntryRequests(),
    loginItem: LoginItem? = null,
): AppContainer = AppContainer(
    RoomStore(database, criticalHits = CriticalHits.Standard),
    files = files,
    reminders = reminders,
    reminderNotices = reminderNotices,
    entryRequests = entryRequests,
    loginItem = loginItem,
)
