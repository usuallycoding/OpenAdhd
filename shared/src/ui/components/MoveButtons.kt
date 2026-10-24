package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.usuallycoding.openadhd.ui.theme.ArrowDownIcon
import com.usuallycoding.openadhd.ui.theme.ArrowUpIcon

/** The move up and move down arrows shown on a row while a list is being reordered. */
@Composable
fun MoveButtons(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row {
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(ArrowUpIcon, contentDescription = "Move $name up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(ArrowDownIcon, contentDescription = "Move $name down")
        }
    }
}
