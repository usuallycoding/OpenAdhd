package com.usuallycoding.openadhd.domain

/** How the app decides between its light and dark appearance. [System] follows the device. */
enum class ThemeMode(val label: String) {
    System("Follow system theme"),
    Light("Light"),
    Dark("Dark");

    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        System -> systemInDarkTheme
        Light -> false
        Dark -> true
    }
}
