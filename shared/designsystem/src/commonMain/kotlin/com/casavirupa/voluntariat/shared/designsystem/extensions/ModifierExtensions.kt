package com.casavirupa.voluntariat.shared.designsystem.extensions

import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

expect fun Modifier.nativeClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier