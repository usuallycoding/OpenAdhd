package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {
    @Test
    fun `system mode follows the device setting`() {
        assertEquals(true, ThemeMode.System.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.System.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `light mode is never dark`() {
        assertEquals(false, ThemeMode.Light.isDark(systemInDarkTheme = true))
        assertEquals(false, ThemeMode.Light.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `dark mode is always dark`() {
        assertEquals(true, ThemeMode.Dark.isDark(systemInDarkTheme = true))
        assertEquals(true, ThemeMode.Dark.isDark(systemInDarkTheme = false))
    }
}
