package com.usuallycoding.openadhd

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.domain.ThemeMode
import com.usuallycoding.openadhd.ui.theme.AppTheme
import com.usuallycoding.openadhd.ui.theme.primaryDark
import com.usuallycoding.openadhd.ui.theme.primaryLight
import com.usuallycoding.openadhd.ui.theme.supportsDynamicColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The Dynamic color switch of [AppTheme]. The desktop has no dynamic color, so it always shows the app's own colors. */
@OptIn(ExperimentalTestApi::class)
class DynamicColorThemeTest {
    @Test
    fun `the desktop offers no dynamic color`() {
        assertFalse(supportsDynamicColor)
    }

    @Test
    fun `turning dynamic color off keeps the app's own colors and applies without a restart`() = runComposeUiTest {
        var dynamic by mutableStateOf(true)
        var mode by mutableStateOf(ThemeMode.Light)
        var primary = Color.Unspecified
        setContent {
            AppTheme(mode, dynamic) { primary = MaterialTheme.colorScheme.primary }
        }
        assertEquals(primaryLight, primary)

        dynamic = false
        waitForIdle()
        assertEquals(primaryLight, primary)

        mode = ThemeMode.Dark
        waitForIdle()
        assertEquals(primaryDark, primary)
    }
}
