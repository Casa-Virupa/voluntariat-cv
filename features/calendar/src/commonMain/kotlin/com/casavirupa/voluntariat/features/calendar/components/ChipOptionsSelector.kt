package com.casavirupa.voluntariat.features.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ChipOptionsSelector(
    options: List<T>,
    selected: T?,
    onOptionSelected: (T) -> Unit,
    displayMode: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipOptionsRow(
        options = options,
        isSelected = { it == selected },
        onOptionClick = onOptionSelected,
        displayMode = displayMode,
        modifier = modifier,
    )
}

@Composable
fun <T> ChipMultiOptionsSelector(
    options: List<T>,
    selected: Set<T>,
    onOptionToggled: (T) -> Unit,
    displayMode: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipOptionsRow(
        options = options,
        isSelected = { it in selected },
        onOptionClick = onOptionToggled,
        displayMode = displayMode,
        modifier = modifier,
    )
}

@Composable
private fun <T> ChipOptionsRow(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onOptionClick: (T) -> Unit,
    displayMode: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val selectedState = isSelected(option)
            FilterChip(
                selected = selectedState,
                onClick = { onOptionClick(option) },
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        displayMode(option)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = CutCornerShape(0.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedState,
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}
