package com.usuallycoding.openadhd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.ui.components.FormSheet
import kotlin.test.Test
import kotlin.test.assertEquals

/** Proves that UI tests can operate inside a bottom sheet, which the form conversions rely on. */
@OptIn(ExperimentalTestApi::class)
class FormSheetTest {
    @Test
    fun `nodes inside the sheet can be typed into scrolled to and clicked and cancel closes it`() = runComposeUiTest {
        var typed = ""
        var reached = 0
        var dismissed = 0
        setContent {
            MaterialTheme {
                var open by remember { mutableStateOf(true) }
                if (open) {
                    FormSheet(
                        title = "Spike",
                        onDismiss = { open = false; dismissed++ },
                        footer = { close -> TextButton(onClick = close) { Text("Cancel") } },
                    ) {
                        var text by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it; typed = it },
                            label = { Text("Field") },
                        )
                        Column {
                            Spacer(Modifier.height(2000.dp))
                            Button(onClick = { reached++ }) { Text("Far down") }
                        }
                    }
                }
            }
        }

        onNodeWithText("Spike").assertIsDisplayed()
        onNodeWithText("Field").performTextInput("hello")
        assertEquals("hello", typed)

        onNodeWithText("Far down").performScrollTo().performClick()
        assertEquals(1, reached)

        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("Spike").assertDoesNotExist()
        assertEquals(1, dismissed)
    }

    @Test
    fun `a header stays visible while the content scrolls`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FormSheet(
                    title = "With header",
                    onDismiss = {},
                    header = { Text("Fixed header") },
                    footer = { close -> TextButton(onClick = close) { Text("Close") } },
                ) {
                    Text("First line")
                    Column {
                        Spacer(Modifier.height(2000.dp))
                        Text("Last line")
                    }
                }
            }
        }

        onNodeWithText("Fixed header").assertIsDisplayed()
        onNodeWithText("First line").assertIsDisplayed()

        onNodeWithText("Last line").performScrollTo().assertIsDisplayed()
        onNodeWithText("Fixed header").assertIsDisplayed()
        onNodeWithText("First line").assertIsNotDisplayed()
    }

    @Test
    fun `a sheet without a header looks as before`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                FormSheet(title = "Plain", onDismiss = {}, footer = { TextButton(onClick = {}) { Text("Close") } }) { Text("Body") }
            }
        }
        onNodeWithText("Plain").assertIsDisplayed()
        onNodeWithText("Body").assertIsDisplayed()
    }
}
