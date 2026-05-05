package com.casavirupa.voluntariat.features.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun CVTag(
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    extraText: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
) {
    val tagText = buildAnnotatedString {
        append(text)
        if (extraText != null) {
            append("· $extraText")
        }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = tagText,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}