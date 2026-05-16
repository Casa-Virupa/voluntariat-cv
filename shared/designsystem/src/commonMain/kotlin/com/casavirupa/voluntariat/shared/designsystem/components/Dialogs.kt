package com.casavirupa.voluntariat.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.shared.designsystem.generated.resources.Res
import voluntariatcv.shared.designsystem.generated.resources.cancel
import voluntariatcv.shared.designsystem.generated.resources.ic_warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningDialog(
    onDismiss: () -> Unit,
    title: String,
    description: String,
    onCancel: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                )
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_warning),
                    contentDescription = null,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CVOutlinedButton(
                        text = stringResource(Res.string.cancel),
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    CVButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}