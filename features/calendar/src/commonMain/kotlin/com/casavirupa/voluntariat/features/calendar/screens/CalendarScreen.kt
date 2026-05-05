package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.features.calendar.viewmodels.CalendarViewModel
import com.casavirupa.voluntariat.features.calendar.components.CalendarPager
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.features.calendar.utils.getName
import com.casavirupa.voluntariat.shared.designsystem.components.CVFabButton
import com.casavirupa.voluntariat.shared.model.calendar.Event
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.ic_add
import voluntariatcv.features.calendar.generated.resources.ic_arrow_left
import voluntariatcv.features.calendar.generated.resources.ic_arrow_right
import voluntariatcv.features.calendar.generated.resources.select_volunteering_title

@Composable
internal fun CalendarScreen(
    onNavToReservationForm: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val volunteers by viewModel.volunteers.collectAsStateWithLifecycle()
    val googleCalendarEvents by viewModel.googleCalendarEvents.collectAsStateWithLifecycle()
    val currentMonth by viewModel.yearMonth.collectAsStateWithLifecycle()

    CalendarContent(
        volunteers = volunteers,
        googleCalendarEvents = googleCalendarEvents,
        yearMonth = currentMonth,
        today = viewModel.todayDate,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onYearMonthChanged = viewModel::onYearMonthChanged,
        onNavToReservationForm = onNavToReservationForm,
        onDayClick = onDayClick,
    )
}

@Composable
private fun CalendarContent(
    volunteers: List<Volunteer>,
    googleCalendarEvents: List<GoogleCalendarEvent>,
    yearMonth: YearMonth,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onYearMonthChanged: (YearMonth) -> Unit,
    onNavToReservationForm: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CalendarTopBar(
                currentMonth = yearMonth.month,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
        },
        floatingActionButton = {
            CVFabButton(
                icon = painterResource(Res.drawable.ic_add),
                onClick = onNavToReservationForm,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            WeekHeader()
            CalendarPager(
                currentReference = yearMonth,
                pageToReference = { base, initialPage, page ->
                    val offsetInMonths = page - initialPage
                    val totalMonths = base.month.number + offsetInMonths - 1

                    val addedYears = totalMonths.floorDiv(TOTAL_MONTHS)
                    val newMonthIndex = totalMonths.mod(TOTAL_MONTHS)

                    YearMonth(
                        year = base.year + addedYears,
                        month = Month(newMonthIndex + 1)
                    )
                },
                calculateOffset = { current, base ->
                    val yearDiff = current.year - base.year
                    val monthDiff = current.month.number - base.month.number
                    (yearDiff * 12) + monthDiff
                },
                modifier = Modifier.weight(1f),
                onReferenceChange = onYearMonthChanged,
            ) { yearMonth ->
                MonthGrid(
                    yearMonth = yearMonth,
                    today = today,
                    googleCalendarEvents = googleCalendarEvents,
                    onDayClick = onDayClick,
                )
            }
        }
    }
}

@Composable
private fun CalendarTopBar(
    currentMonth: Month,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 32.dp, bottom = 16.dp, start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarTitles(
            title = currentMonth.getName(),
            modifier = Modifier.weight(2f)
        )
        CalendarNavigationArrows(
            onClickPrevious = onPreviousMonth,
            onClickNext = onNextMonth,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun TopBarTitles(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 600)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 600))
            },
            label = "MonthTitleAnimation"
        ) { animatedTitle ->
            Text(
                text = animatedTitle,
                modifier = Modifier.padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.displaySmall,
            )
        }
        Text(
            text = stringResource(Res.string.select_volunteering_title).uppercase(),
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
    onDayClick: (LocalDate) -> Unit,
    today: LocalDate,
    googleCalendarEvents: List<GoogleCalendarEvent>,
    modifier: Modifier = Modifier,
) {
    val calendarDays = remember(yearMonth) {
        buildList {
            for (i in 0 until yearMonth.firstDayOfWeek) {
                val ordinal = yearMonth.daysInPrevMonth - (yearMonth.firstDayOfWeek - i - 1)
                add(Pair(LocalDate(yearMonth.prevYear, yearMonth.prevMonth, ordinal), false))
            }
            for (day in 1..yearMonth.daysInMonth) {
                add(Pair(LocalDate(yearMonth.year, yearMonth.month, day), true))
            }

            val remaining = TOTAL_DAYS_SHOWED_IN_CALENDAR - size
            for (day in 1..remaining) {
                add(Pair(LocalDate(yearMonth.nextYear, yearMonth.nextMonth, day), false))
            }
        }
    }

    BoxWithConstraints(propagateMinConstraints = true, modifier = modifier) {
        val dayCellSize = remember(maxWidth, maxHeight) {
            DpSize(width = maxWidth / 7, height = maxHeight/ 6)
        }

        Column {
            for (row in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val (date, isCurrentMonth) = calendarDays[index]
                        val googleCalendarEvent = googleCalendarEvents.firstOrNull {
                            it.start.day == date.day && it.start.month == date.month
                        }
                        DayCell(
                            date = date,
                            isCurrentMonth = isCurrentMonth,
                            isPast = date < today,
                            today = today,
                            googleCalendarEvent = googleCalendarEvent,
                            cellSize = dayCellSize,
                            onClick = { onDayClick(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isPast: Boolean,
    today: LocalDate,
    cellSize: DpSize,
    googleCalendarEvent: GoogleCalendarEvent?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = date == today
    Box(
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CutCornerShape(0.dp),
            )
            .size(cellSize)
            .then(if (isPast) Modifier else Modifier.clickable { onClick() }),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
            )
            if (googleCalendarEvent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height(6.dp)
                        .background(Color.Blue)
                )
            }
        }
    }
}

private const val TOTAL_DAYS_SHOWED_IN_CALENDAR = 42
private const val TOTAL_MONTHS = 12
