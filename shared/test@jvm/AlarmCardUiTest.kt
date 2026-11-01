package com.usuallycoding.openadhd

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.usuallycoding.openadhd.data.AlarmCard
import com.usuallycoding.openadhd.data.AlarmRow
import com.usuallycoding.openadhd.ui.theme.AppTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** The alarm card: its rows, the "Due" line and the buttons of each row. */
@OptIn(ExperimentalTestApi::class)
class AlarmCardUiTest {
    private val started = Instant.parse("2026-10-23T10:00:00Z")
    private val day = LocalDate(2026, 10, 23)

    private fun row(id: String, text: String, dueAt: Instant? = null) = AlarmRow(id, text, started, dueAt, day)

    @Test
    fun `one row shows the name and both buttons`() = runComposeUiTest {
        setContent { AppTheme { AlarmCard(listOf(row("a", "Take medication")), {}, {}, timeZone = TimeZone.UTC) } }

        onNodeWithText("Take medication").assertIsDisplayed()
        onNodeWithTag("alarm-open-a").assertIsDisplayed()
        onNodeWithTag("alarm-dismiss-a").assertIsDisplayed()
        onNodeWithText("Open task").assertIsDisplayed()
        onNodeWithText("Dismiss").assertIsDisplayed()
    }

    @Test
    fun `two rows are listed together`() = runComposeUiTest {
        setContent {
            AppTheme { AlarmCard(listOf(row("a", "Take medication"), row("b", "Taxes")), {}, {}, timeZone = TimeZone.UTC) }
        }

        onNodeWithTag("alarm-row-a").assertIsDisplayed()
        onNodeWithTag("alarm-row-b").assertIsDisplayed()
        onNodeWithText("Taxes").assertIsDisplayed()
    }

    @Test
    fun `the due time is shown only for a row that rang late`() = runComposeUiTest {
        val due = Instant.parse("2026-10-23T07:30:00Z")
        setContent {
            AppTheme {
                AlarmCard(listOf(row("a", "Take medication", dueAt = due), row("b", "Taxes")), {}, {}, timeZone = TimeZone.UTC)
            }
        }

        onNodeWithText("Due 07:30").assertIsDisplayed()
        assertEquals(1, onAllNodesWithTextCount("Due"))
    }

    @Test
    fun `each button acts on its own row`() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val dismissed = mutableListOf<String>()
        setContent {
            AppTheme {
                AlarmCard(listOf(row("a", "Take medication"), row("b", "Taxes")), { opened += it }, { dismissed += it }, timeZone = TimeZone.UTC)
            }
        }

        onNodeWithTag("alarm-open-b").performClick()
        onNodeWithTag("alarm-dismiss-a").performClick()
        assertEquals(listOf("b"), opened)
        assertEquals(listOf("a"), dismissed)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.onAllNodesWithTextCount(prefix: String): Int =
        onAllNodes(androidx.compose.ui.test.hasText(prefix, substring = true)).fetchSemanticsNodes().size
}
