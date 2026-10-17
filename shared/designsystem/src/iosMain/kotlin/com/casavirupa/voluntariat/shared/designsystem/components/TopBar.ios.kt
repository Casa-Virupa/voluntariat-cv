package com.casavirupa.voluntariat.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
    actions: (@Composable RowScope.() -> Unit)?,
) {
    Row(
        modifier =  modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .padding(end = 4.dp, bottom = 4.dp)
                .weight(1f),
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
        if (actions != null) {
            Row {
                actions()
            }
        }
    }
}
