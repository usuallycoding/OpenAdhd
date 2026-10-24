package com.usuallycoding.openadhd.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.usuallycoding.openadhd.domain.GameRules

/** The rules in force, for screens that print amounts. [App] provides the user's settings; the default is the original rules. */
val LocalGameRules = staticCompositionLocalOf { GameRules.Default }
