package com.usuallycoding.openadhd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usuallycoding.openadhd.domain.HistoryDays
import com.usuallycoding.openadhd.ui.theme.ChevronIcon
import kotlinx.datetime.LocalDate

private val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * A month grid for picking a day: [selected] is highlighted, days in [markedDays] carry a dot, and days that
 * [HistoryDays.isSelectable] rejects are dimmed and cannot be tapped. It opens on the month of [selected] and
 * can page back without limit but not past the month of [today]. [markedLabel] names what marked days have.
 */
@Composable
fun DayCalendar(
    selected: LocalDate,
    today: LocalDate,
    markedDays: Set<LocalDate>,
    includeToday: Boolean,
    markedLabel: String,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var month by remember { mutableStateOf(HistoryDays.monthStart(selected)) }
    val lastMonth = HistoryDays.monthStart(today)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { month = HistoryDays.addMonths(month, -1) }) {
                Icon(ChevronIcon, contentDescription = "Previous month", modifier = Modifier.rotate(90f))
            }
            Text(month.monthLabel(), style = MaterialTheme.typography.titleMedium)
            // Kept in the layout when hidden so the month name does not move.
            val canGoNext = month < lastMonth
            IconButton(onClick = { month = HistoryDays.addMonths(month, 1) }, enabled = canGoNext) {
                if (canGoNext) Icon(ChevronIcon, contentDescription = "Next month", modifier = Modifier.rotate(-90f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        HistoryDays.monthGrid(month).chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DayCell(
                            day = day,
                            isSelected = day == selected,
                            isMarked = day in markedDays,
                            enabled = HistoryDays.isSelectable(day, today, includeToday),
                            markedLabel = markedLabel,
                            onClick = { onSelect(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    isSelected: Boolean,
    isMarked: Boolean,
    enabled: Boolean,
    markedLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val description = day.fullLabel() + if (isMarked) ", has $markedLabel" else ""
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.aspectRatio(1f).padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) colors.primary else colors.surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(if (enabled) 1f else 0.38f)) {
            Text(
                day.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) colors.onPrimary else colors.onSurface,
            )
            Box(
                modifier = Modifier.size(5.dp).background(
                    if (isMarked) (if (isSelected) colors.onPrimary else colors.primary) else colors.surface.copy(alpha = 0f),
                    CircleShape,
                ),
            )
        }
    }
}

/** For example "October 2026". */
private fun LocalDate.monthLabel(): String = "${monthName()} $year"

/** For example "Thursday, October 22, 2026". */
private fun LocalDate.fullLabel(): String =
    "${dayOfWeek.name.titleCase()}, ${monthName()} $day, $year"

private fun LocalDate.monthName(): String = month.name.titleCase()

private fun String.titleCase(): String = lowercase().replaceFirstChar { it.uppercase() }
