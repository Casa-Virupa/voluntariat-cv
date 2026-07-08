package com.casavirupa.voluntariat.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun MediumTopBar(
    title: String,
    modifier: Modifier,
    navigationIcon: (@Composable (() -> Unit))?,
    subtitle: String?,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
            .padding(end = 4.dp, top = 12.dp, bottom = 4.dp),
    ) {
        if (navigationIcon != null) {
            navigationIcon()
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}
