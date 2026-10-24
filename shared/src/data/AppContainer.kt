package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.ui.today.DefaultDateCheckInterval
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
)

/**
 * The real app's wiring, shared by every platform entry point: a Room-backed store with critical hits switched on.
 * Stores default to no critical hits, so this is the one place that turns them on for the running app.
 */
fun createContainer(database: OpenAdhdDatabase, files: DataFiles? = null): AppContainer =
    AppContainer(RoomStore(database, criticalHits = CriticalHits.Standard), files = files)
