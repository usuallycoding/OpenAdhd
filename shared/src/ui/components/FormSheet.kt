package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A bottom sheet for forms and pickers. The [content] scrolls when it is taller than the available height, while
 * the [footer] with the Save/Cancel (or Close) actions stays fixed at the bottom of the sheet. Swiping the sheet
 * down, tapping outside it and the footer's `requestClose` all end in [onDismiss]; `requestClose` slides the sheet
 * out first. An optional [header] stays fixed above the scrolling content, for controls such as a search field
 * that must stay reachable however far the content is scrolled. The sheet opens fully expanded and keeps the focused field above the on-screen keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    title: String,
    onDismiss: () -> Unit,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    /**
     * Takes focus out of the field, which puts the keyboard away, when the body is tapped or scrolled. Off by default:
     * a form's body holds text fields, and a tap on one of them must not clear the focus it has just taken. Meant for
     * a sheet whose search field is in the [header] and whose body only lists results. Whatever this is set to, a
     * downward scroll on the body while the keyboard is open puts away only the keyboard and leaves the sheet open.
     */
    dismissKeyboardOnBodyInteraction: Boolean = false,
    footer: @Composable RowScope.(requestClose: () -> Unit) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val keyboardShowing by rememberKeyboardShowing()
    // While the keyboard is showing the sheet refuses to move to Hidden and puts the keyboard away instead, so a swipe on
    // its title or footer, or a tap on the dimmed area, closes only the keyboard.
    val gate = remember(focus, keyboard) { SheetDismissGate({ keyboardShowing }, { focus.putKeyboardAway(keyboard) }) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> gate.allows(movingToHidden = target == SheetValue.Hidden) },
    )
    val scope = rememberCoroutineScope()
    val requestClose: () -> Unit = {
        gate.closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            if (header != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    content = header,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .putKeyboardAwayOnScroll()
                    .then(if (dismissKeyboardOnBodyInteraction) Modifier.dismissKeyboardOnTapOrScroll() else Modifier)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                content = content,
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                footer(requestClose)
            }
        }
    }
}
