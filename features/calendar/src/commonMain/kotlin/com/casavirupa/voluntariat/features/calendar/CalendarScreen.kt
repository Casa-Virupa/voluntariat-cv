package com.casavirupa.voluntariat.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.features.calendar.utils.MonthCalculations
import com.casavirupa.voluntariat.shared.designsystem.components.CVFabButton
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.ic_add
import voluntariatcv.features.calendar.generated.resources.ic_arrow_left
import voluntariatcv.features.calendar.generated.resources.ic_arrow_right
import kotlin.time.Clock

@Composable
internal fun CalendarScreen() {
    CalendarContent()
}

@Composable
private fun CalendarContent() {
    Scaffold(
        topBar = {
            CalendarTopBar()
        },
        floatingActionButton = {
            CVFabButton(
                icon = painterResource(Res.drawable.ic_add),
                onClick = {},
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            WeekHeader()
            MonthGrid(
                yearMonth = YearMonth(2026, Month.MARCH),
                onClickDay = {},
            )
        }
    }
}

@Composable
private fun CalendarTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 32.dp, bottom = 16.dp, start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarTitles(modifier = Modifier.weight(2f))
        CalendarNavigationArrows(
            onClickPrevious = {},
            onClickNext = {},
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun TopBarTitles(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Octubre",
            modifier = Modifier.padding(bottom = 4.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = "Selecciona el dia de voluntariat".uppercase(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CalendarNavigationArrows(
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onClickPrevious) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
        IconButton(onClick = onClickNext) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

@Composable
private fun WeekHeader(modifier: Modifier = Modifier) {
    val weekDayNames = listOf("Dl", "Dt", "Dc", "Dj", "Dv", "Ds", "Dm")
    val borderColor = Color(0xFFE7E5E4)

    Column(modifier = modifier) {
        HorizontalDivider(color = borderColor)
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayNames.forEach { dayName ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = dayName.uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        HorizontalDivider(color = borderColor)
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    onClickDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    val skipPreviousPadding = yearMonth.firstDayOfWeek >= 7
    val totalDaysDisplayed = if (skipPreviousPadding) {
        yearMonth.daysInMonth
    } else {
        yearMonth.firstDayOfWeek + yearMonth.daysInMonth
    }
    val remainingCells = TOTAL_DAYS_SHOWED_IN_CALENDAR - totalDaysDisplayed

    val monthCalculations = remember(yearMonth) {
        MonthCalculations(yearMonth.month, yearMonth.year)
    }

    BoxWithConstraints(propagateMinConstraints = true) {
        // Cache day cell size to avoid recalculation
        val dayCellSize = remember(maxWidth, maxHeight) {
            DpSize(
                width = maxWidth.div(7),
                height = (maxHeight - 50.dp).div(6),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = modifier,
            state = gridState,
            userScrollEnabled = false,
        ) {
            with(monthCalculations) {
                if (yearMonth.firstDayOfWeek > 0 && !skipPreviousPadding) {
                    items(
                        count = yearMonth.firstDayOfWeek,
                        key = { index ->
                            val ordinal = daysInPrevMonth - (yearMonth.firstDayOfWeek - index - 1)
                            "prev_${prevYear}_${prevMonth.number}_$ordinal"
                        },
                    ) { index ->
                        val ordinal = daysInPrevMonth - (yearMonth.firstDayOfWeek - index - 1)
                        val date = LocalDate(prevYear, prevMonth, ordinal)
                        DayCell(
                            date = date,
                            isCurrentMonth = false,
                            cellSize = dayCellSize,
                            onClick = onClickDay,
                        )
                    }
                }
                items(
                    count = yearMonth.daysInMonth,
                    key = { day ->
                        "current_${yearMonth.year}_${yearMonth.month.number}_${day + 1}"
                    },
                ) { day ->
                    val date = LocalDate(yearMonth.year, yearMonth.month, day + 1)
                    DayCell(
                        date = date,
                        isCurrentMonth = true,
                        cellSize = dayCellSize,
                        onClick = onClickDay,
                    )
                }
                items(
                    count = remainingCells,
                    key = { day -> "next_${nextYear}_${nextMonth.number}_${day + 1}" },
                ) { day ->
                    val date = LocalDate(nextYear, nextMonth, day + 1)
                    DayCell(
                        date = date,
                        isCurrentMonth = false,
                        cellSize = dayCellSize,
                        onClick = onClickDay,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    cellSize: DpSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    val isToday = date == today
    Box(
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CutCornerShape(0.dp),
            )
            .size(cellSize)
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter,
    ) {
        val selectionBackgroundColor = if (isToday) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        }
        Text(
            text = date.day.toString(),
            modifier = Modifier
                .padding(8.dp)
                .background(color = selectionBackgroundColor, shape = CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.titleSmall,
            color =
                when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            textAlign = TextAlign.Center,
        )
    }
}

private const val TOTAL_DAYS_SHOWED_IN_CALENDAR = 42
