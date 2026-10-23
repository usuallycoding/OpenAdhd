package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.ui.today.DefaultDateCheckInterval
import kotlin.time.Clock
import kotlin.time.Duration

/** The app's long-lived dependencies, created once per platform entry point. */
class AppContainer(
    val store: OpenAdhdStore,
    val clock: Clock = Clock.System,
    val dateCheckInterval: Duration? = DefaultDateCheckInterval,
)
