package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.shared.designsystem.extensions.nativeClickable
import com.casavirupa.voluntariat.shared.model.calendar.CalendarFilter
import com.mohamedrejeb.calf.ui.sheet.AdaptiveBottomSheet
import com.mohamedrejeb.calf.ui.sheet.rememberAdaptiveSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarFiltersModal(
    allFilters: List<CalendarFilter>,
    currentFilter: CalendarFilter,
    onSelectFilter: (CalendarFilter) -> Unit,
    onCloseFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberAdaptiveSheetState()

    AdaptiveBottomSheet(
        onDismissRequest = onCloseFilters,
        modifier = modifier,
        adaptiveSheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            allFilters.forEach { filter ->
                FilterOption(
                    text = filter.getText(),
                    isSelected = currentFilter == filter,
                    onClick = { onSelectFilter(filter) }
                )
            }
        }
    }
}

@Composable
private fun FilterOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .nativeClickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        val background = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        }
        Row(
            modifier = Modifier
                .background(background, shape = RoundedCornerShape(4.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
            )
            if (isSelected) {

            }
        }
    }
}

@Composable
private fun CalendarFilter.getText() =
    when (this) {
        CalendarFilter.SeeAll -> "Veure-ho tot"
        CalendarFilter.MyVolunteers -> "Els teus voluntariats"
        CalendarFilter.OtherVolunteers -> "Tots els voluntariats"
    }
