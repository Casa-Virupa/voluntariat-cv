package com.casavirupa.voluntariat.features.calendar.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> CalendarPager(
    currentReference: T,
    calculateOffset: (current: T, base: T) -> Int,
    pageToReference: (baseReference: T, initialPage: Int, page: Int) -> T,
    onReferenceChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (reference: T) -> Unit,
) {
    val initialPage = TOTAL_OF_MONTHS / 2

    // Capture initial reference as stable base for offset calculations
    val baseReference = remember { currentReference }

    val referenceOffset =
        remember(currentReference, baseReference) {
            calculateOffset(currentReference, baseReference)
        }

    val pagerState =
        rememberPagerState(
            initialPage = initialPage + referenceOffset,
            pageCount = { TOTAL_OF_MONTHS },
        )

    val pageConverter: (Int) -> T =
        remember(baseReference, initialPage) {
            { page ->
                pageToReference(baseReference, initialPage, page)
            }
        }

    val currentReferenceState = rememberUpdatedState(currentReference)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                val newReference = pageConverter(page)
                if (newReference != currentReferenceState.value) {
                    onReferenceChange(newReference)
                }
            }
    }

    LaunchedEffect(currentReference) {
        val targetOffset = calculateOffset(currentReference, baseReference)
        val targetPage = initialPage + targetOffset

        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        pageSpacing = 8.dp,
        beyondViewportPageCount = 0,
    ) { page ->
        val reference = pageConverter(page)
        content(reference)
    }
}

private const val TOTAL_OF_MONTHS = 200