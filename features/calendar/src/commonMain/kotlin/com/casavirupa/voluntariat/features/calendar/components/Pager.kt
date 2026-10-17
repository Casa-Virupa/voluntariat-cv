package com.casavirupa.voluntariat.features.calendar.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Month

/**
 * Horizontal pager over months. Pages map to months through a fixed anchor, so a page index
 * means the same month in every composition: the pager state is `rememberSaveable` and is
 * restored (ignoring `initialPage`) when the calendar comes back from the day detail, while
 * the ViewModel keeps the month on its own — both must agree without any shared "base" month.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CalendarPager(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (month: YearMonth) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = currentMonth.toPage(),
        pageCount = { PAGE_COUNT },
    )
    val latestMonth by rememberUpdatedState(currentMonth)

    // Pager → ViewModel. The settled page is the source of truth both for swipes and for a
    // restored pager (after process death the ViewModel is back to today, the pager is not).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .map { page -> page.toYearMonth() }
            .collect { month ->
                if (month != latestMonth) {
                    onMonthChange(month)
                }
            }
    }

    // ViewModel → pager (arrow buttons). drop(1) skips the value already present when the
    // screen is (re)entered, so nothing scrolls on the way back from the day detail.
    LaunchedEffect(Unit) {
        snapshotFlow { latestMonth }
            .drop(1)
            .collectLatest { month ->
                val targetPage = month.toPage()
                if (pagerState.currentPage != targetPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        pageSpacing = 8.dp,
        beyondViewportPageCount = 0,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = 0.2f,
        ),
    ) { page ->
        content(page.toYearMonth())
    }
}

// Page 0 is January 2000; the last page is December 2099.
private val ANCHOR = YearMonth(2000, Month.JANUARY)
private const val PAGE_COUNT = 12 * 100

private fun YearMonth.toPage(): Int =
    (monthIndex - ANCHOR.monthIndex).coerceIn(0, PAGE_COUNT - 1)

private fun Int.toYearMonth(): YearMonth =
    YearMonth.fromMonthIndex(ANCHOR.monthIndex + this)
