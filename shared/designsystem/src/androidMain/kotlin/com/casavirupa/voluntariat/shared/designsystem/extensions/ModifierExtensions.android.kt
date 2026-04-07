package com.casavirupa.voluntariat.shared.designsystem.extensions

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

actual fun Modifier.nativeClickable(
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit
): Modifier = composed {
    val finalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val finalIndication = indication ?: LocalIndication.current

    clickable(
        interactionSource = finalInteractionSource,
        indication = finalIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
}