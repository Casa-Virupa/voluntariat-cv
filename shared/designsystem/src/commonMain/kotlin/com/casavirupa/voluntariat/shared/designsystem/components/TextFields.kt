package com.casavirupa.voluntariat.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.designsystem.extensions.nativeClickable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
fun CVTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholder: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    showError: Boolean = false,
    supportingText: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
) {
    val unselectedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedBorderColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    var borderColor by remember { mutableStateOf(unselectedBorderColor) }
    var borderWidth by remember { mutableStateOf(1.dp) }

    BasicTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = modifier.onFocusChanged {
            borderColor = if (it.isFocused) {
                selectedBorderColor
            } else {
                unselectedBorderColor
            }
            borderWidth = when {
                it.isFocused -> 1.5.dp
                else -> 1.dp
            }
        },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        decorationBox = { innerTextField ->
            Column {
                if (label != null) {
                    Text(
                        text = label.uppercase(),
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                val textFieldBorderColor = if (showError) {
                    errorColor
                } else {
                    borderColor
                }
                Row(
                    modifier = Modifier
                        .border(width = borderWidth, color = textFieldBorderColor)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            leadingIcon()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(modifier = Modifier.padding(start = 16.dp)) {
                            innerTextField()
                        }
                        if (!placeholder.isNullOrBlank() && value.isEmpty()) {
                            Text(
                                text = placeholder,
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (trailingIcon != null) {
                        trailingIcon()
                    }
                }
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        color = errorColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                        )
                    )
                }
            }
        }
    )
}

@Composable
fun DateTextField(
    date: LocalDate?,
    onDateChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    pattern: String = "",
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        CVTextField(
            value = date?.format(pattern).orEmpty(),
            onValueChanged = {},
            modifier = Modifier
                .fillMaxWidth()
                .nativeClickable(onClick = { showDatePicker = true }),
            placeholder = placeholder,
            readOnly = true,
            enabled = false,
        )
        if (showDatePicker) {
            NativeDatePicker(
                date = date,
                onDateSelected = {
                    it?.let(onDateChanged)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@Composable
fun TimeTextField(
    time: LocalTime?,
    onTimeChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: Painter? = null,
    pattern: String? = null,
    enabled: Boolean = true,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    val timeString = if (pattern == null) {
        time?.toString().orEmpty()
    } else {
        time?.format(pattern).orEmpty()
    }

    Column(modifier = modifier) {
        CVTextField(
            value = timeString,
            onValueChanged = {},
            modifier = Modifier
                .fillMaxWidth()
                .nativeClickable(
                    enabled = enabled,
                    onClick = { showTimePicker = true },
                ),
            placeholder = placeholder,
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        painter = it,
                        contentDescription = null,
                    )
                }
            },
            readOnly = true,
            enabled = false,
        )
        if (showTimePicker) {
            NativeTimePicker(
                initialTime = time,
                onTimeSelected = {
                    onTimeChanged(it)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false },
            )
        }
    }
}