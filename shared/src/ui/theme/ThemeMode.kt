package com.usuallycoding.openadhd.ui.theme

enum class ThemeMode {
    System,
    Light,
    Dark;

    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        System -> systemInDarkTheme
        Light -> false
        Dark -> true
    }
}
