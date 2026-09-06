package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.features.calendar.components.CalendarPager
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.features.calendar.utils.getName
import com.casavirupa.voluntariat.features.calendar.viewmodels.CalendarViewModel
import com.casavirupa.voluntariat.shared.designsystem.components.CVFabButton
import com.casavirupa.voluntariat.shared.model.calendar.CalendarFilter
import com.casavirupa.voluntariat.shared.model.calendar.DayCoverage
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.isHappeningOn
import com.casavirupa.voluntariat.shared.model.calendar.unavailabilityOn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.filter_general_volunteering
import voluntariatcv.features.calendar.generated.resources.filter_my_volunteering
import voluntariatcv.features.calendar.generated.resources.filter_specific_volunteering
import voluntariatcv.features.calendar.generated.resources.friday_short
import voluntariatcv.features.calendar.generated.resources.ic_add
import voluntariatcv.features.calendar.generated.resources.ic_arrow_left
import voluntariatcv.features.calendar.generated.resources.ic_arrow_right
import voluntariatcv.features.calendar.generated.resources.ic_filter
import voluntariatcv.features.calendar.generated.resources.monday_short
import voluntariatcv.features.calendar.generated.resources.saturday_short
import voluntariatcv.features.calendar.generated.resources.sunday_short
import voluntariatcv.features.calendar.generated.resources.thursday_short
import voluntariatcv.features.calendar.generated.resources.tuesday_short
import voluntariatcv.features.calendar.generated.resources.volunteers_count
import voluntariatcv.features.calendar.generated.resources.wednesday_short

@Composable
internal fun CalendarScreen(
    onNavToReservationForm: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val volunteers by viewModel.volunteers.collectAsStateWithLifecycle()
    val myVolunteerDates by viewModel.myVolunteerDates.collectAsStateWithLifecycle()
    val googleCalendarEvents by viewModel.googleCalendarEvents.collectAsStateWithLifecycle()
    val currentMonth by viewModel.yearMonth.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    CalendarContent(
        volunteers = volunteers,
        myVolunteerDates = myVolunteerDates,
        googleCalendarEvents = googleCalendarEvents,
        yearMonth = currentMonth,
        today = viewModel.todayDate,
        filter = filter,
        onFilterSelected = viewModel::onFilterSelected,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onYearMonthChanged = viewModel::onYearMonthChanged,
        onNavToReservationForm = onNavToReservationForm,
        onDayClick = onDayClick,
    )
}

@Composable
private fun CalendarContent(
    volunteers: Map<LocalDate, Int>,
    myVolunteerDates: Set<LocalDate>,
    googleCalendarEvents: List<GoogleCalendarEvent>,
    yearMonth: YearMonth,
    today: LocalDate,
    filter: CalendarFilter?,
    onFilterSelected: (CalendarFilter) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onYearMonthChanged: (YearMonth) -> Unit,
    onNavToReservationForm: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // UI-only: whether the filter chips are shown in place of the weekday row
    var filtersVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CalendarTopBar(
                currentMonth = yearMonth.month,
                filtersVisible = filtersVisible,
                hasActiveFilter = filter != null,
                onToggleFilters = { filtersVisible = !filtersVisible },
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
            FiltersOrWeekHeader(
                filtersVisible = filtersVisible,
                filter = filter,
                onFilterSelected = onFilterSelected,
            )
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
                    volunteers = volunteers,
                    myVolunteerDates = myVolunteerDates,
                )
            }
        }
    }
}

@Composable
private fun CalendarTopBar(
    currentMonth: Month,
    filtersVisible: Boolean,
    hasActiveFilter: Boolean,
    onToggleFilters: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = 32.dp, bottom = 8.dp, start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarTitle(
            title = currentMonth.getName(),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleFilters) {
            Icon(
                painter = painterResource(Res.drawable.ic_filter),
                contentDescription = null,
                // Stays tinted while a filter is active so it's visible even with the chips hidden
                tint = if (filtersVisible || hasActiveFilter) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.inverseOnSurface
                },
            )
        }
        CalendarNavigationArrows(
            onClickPrevious = onPreviousMonth,
            onClickNext = onNextMonth,
        )
    }
}

@Composable
private fun TopBarTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = title,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 600)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 600))
        },
        label = "MonthTitleAnimation",
        modifier = modifier,
    ) { animatedTitle ->
        Text(
            text = animatedTitle,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 28.sp,
                lineHeight = 32.sp,
            ),
        )
    }
}

@Composable
private fun CalendarNavigationArrows(
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
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

/**
 * The strip under the month title: the weekday names by default, or the filter chips
 * when the filter icon is toggled. Both share a fixed height so the grid never jumps.
 */
@Composable
private fun FiltersOrWeekHeader(
    filtersVisible: Boolean,
    filter: CalendarFilter?,
    onFilterSelected: (CalendarFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Column(modifier = modifier) {
        HorizontalDivider(color = borderColor)
        AnimatedContent(
            targetState = filtersVisible,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300))
            },
            label = "FiltersOrWeekHeaderAnimation",
        ) { showFilters ->
            if (showFilters) {
                CalendarFilters(
                    selected = filter,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier
                        .height(HEADER_STRIP_HEIGHT)
                        .padding(horizontal = 16.dp),
                )
            } else {
                WeekHeader(modifier = Modifier.height(HEADER_STRIP_HEIGHT))
            }
        }
        HorizontalDivider(color = borderColor)
    }
}

@Composable
private fun CalendarFilters(
    selected: CalendarFilter?,
    onFilterSelected: (CalendarFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.displayName(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun CalendarFilter.displayName() =
    when (this) {
        CalendarFilter.General -> stringResource(Res.string.filter_general_volunteering)
        CalendarFilter.Specific -> stringResource(Res.string.filter_specific_volunteering)
        CalendarFilter.Mine -> stringResource(Res.string.filter_my_volunteering)
    }

@Composable
private fun WeekHeader(modifier: Modifier = Modifier) {
    val weekDayNames = listOf(
        stringResource(Res.string.monday_short),
        stringResource(Res.string.tuesday_short),
        stringResource(Res.string.wednesday_short),
        stringResource(Res.string.thursday_short),
        stringResource(Res.string.friday_short),
        stringResource(Res.string.saturday_short),
        stringResource(Res.string.sunday_short),
    )

    Row(modifier = modifier.fillMaxWidth()) {
        weekDayNames.forEach { dayName ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
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
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    onDayClick: (LocalDate) -> Unit,
    today: LocalDate,
    googleCalendarEvents: List<GoogleCalendarEvent>,
    volunteers: Map<LocalDate, Int>,
    myVolunteerDates: Set<LocalDate>,
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

        val eventsByDate = remember(googleCalendarEvents, calendarDays) {
            calendarDays.associate { (date, _) ->
                date to googleCalendarEvents.filter { it.isHappeningOn(date) }
            }
        }

        Column {
            for (row in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val (date, isCurrentMonth) = calendarDays[index]
                        val googleCalendarEventsForDay = eventsByDate[date] ?: emptyList()
                        DayCell(
                            date = date,
                            isCurrentMonth = isCurrentMonth,
                            isPast = date < today,
                            today = today,
                            googleCalendarEvents = googleCalendarEventsForDay,
                            cellSize = dayCellSize,
                            numOfVolunteers = volunteers[date],
                            isMine = date in myVolunteerDates,
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
    googleCalendarEvents: List<GoogleCalendarEvent>,
    onClick: () -> Unit,
    numOfVolunteers: Int?,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    val isToday = date == today
    // A "NO VOLUNTARIAT" with hours only greys the half of the day it falls in
    // (morning: top-left triangle, afternoon: bottom-right); an all-day one greys it all.
    val unavailability = remember(googleCalendarEvents, date) {
        googleCalendarEvents.unavailabilityOn(date)
    }
    val isFullyUnavailable = unavailability == DayCoverage.WholeDay
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val unavailableColor = Color.Gray.copy(alpha = 0.1f)
    // The viewer's own volunteering days are tinted brand blue (tertiary)
    val mineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f)

    Box(
        modifier = modifier
            .size(cellSize)
            .drawBehind {
                if (isMine) {
                    drawRect(color = mineColor)
                }
                if (!isPast) {
                    when (unavailability) {
                        DayCoverage.WholeDay -> drawRect(color = unavailableColor)
                        DayCoverage.Morning -> drawPath(
                            path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(0f, size.height)
                                close()
                            },
                            color = unavailableColor,
                        )
                        DayCoverage.Afternoon -> drawPath(
                            path = Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            },
                            color = unavailableColor,
                        )
                        DayCoverage.None -> Unit
                    }
                }
                val strokeWidth = 0.5.dp.toPx()
                // Draw bottom and right borders for the grid effect
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = borderColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .then(if (isPast) Modifier else Modifier.clickable { onClick() }),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val selectionBackgroundColor = if (isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
            Text(
                text = date.day.toString(),
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 5.dp)
                    .background(color = selectionBackgroundColor, shape = CircleShape)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color =
                    when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isMine -> MaterialTheme.colorScheme.tertiary
                        isFullyUnavailable -> MaterialTheme.colorScheme.onSurfaceVariant
                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
            )
            if (numOfVolunteers != null) {
                CalendarEvent(
                    text = pluralStringResource(
                        Res.plurals.volunteers_count,
                        numOfVolunteers,
                        numOfVolunteers,
                    )
                )
            }
            if (!isPast && !isFullyUnavailable) {
                googleCalendarEvents
                    .filter { it.available }
                    .forEach {
                        CalendarEvent(
                            text = it.title,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
            }
        }
    }
}

@Composable
private fun CalendarEvent(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(2.dp)
            ).padding(horizontal = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.sp),
        color = MaterialTheme.colorScheme.onPrimary,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

private const val TOTAL_DAYS_SHOWED_IN_CALENDAR = 42
private const val TOTAL_MONTHS = 12

// Weekday row was 12dp padding + one labelSmall line; the filter chips (32dp) fit in it too
private val HEADER_STRIP_HEIGHT = 44.dp
