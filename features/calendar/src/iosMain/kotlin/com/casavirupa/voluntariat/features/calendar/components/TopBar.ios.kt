package com.casavirupa.voluntariat.features.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
actual fun MediumTopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit),
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
            .padding(end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}
