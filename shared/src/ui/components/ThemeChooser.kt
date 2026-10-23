package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.usuallycoding.openadhd.ui.theme.DarkThemeIcon
import com.usuallycoding.openadhd.ui.theme.LightThemeIcon
import com.usuallycoding.openadhd.ui.theme.SystemThemeIcon
import com.usuallycoding.openadhd.ui.theme.ThemeMode

private data class ThemeOption(val mode: ThemeMode, val icon: ImageVector, val label: String)

private val themeOptions = listOf(
    ThemeOption(ThemeMode.System, SystemThemeIcon, "Follow system theme"),
    ThemeOption(ThemeMode.Light, LightThemeIcon, "Light"),
    ThemeOption(ThemeMode.Dark, DarkThemeIcon, "Dark"),
)

@Composable
fun ThemeChooser(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = themeOptions.first { it.mode == selected }

    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(current.icon, contentDescription = "Theme: ${current.label}")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        themeOptions.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = { Icon(option.icon, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSelected(option.mode)
                },
                modifier = if (option.mode == selected) {
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                },
            )
        }
    }
}
