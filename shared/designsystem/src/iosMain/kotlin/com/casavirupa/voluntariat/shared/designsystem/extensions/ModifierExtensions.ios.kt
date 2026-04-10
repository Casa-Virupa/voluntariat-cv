package com.casavirupa.voluntariat.shared.designsystem.extensions

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

actual fun Modifier.nativeClickable(
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit
): Modifier = applyIosEffect(interactionSource) { finalInteractionSource ->
    clickable(
        interactionSource = finalInteractionSource,
        indication = null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
}

@Suppress("ModifierComposed")
private fun Modifier.applyIosEffect(
    interactionSource: MutableInteractionSource?,
    block: Modifier.(MutableInteractionSource) -> Modifier,
) = composed {
    val finalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by finalInteractionSource.collectIsPressedAsState()

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_ALPHA else RELEASED_ALPHA,
        label = "Alpha Animation",
        animationSpec = tween(
            durationMillis = if (isPressed) {
                RELEASED_TO_PRESSED_MILLIS
            } else {
                PRESSED_TO_RELEASED_MILLIS
            },
            easing = FastOutSlowInEasing,
        ),
    )

    graphicsLayer {
        alpha = animatedAlpha
    }.block(finalInteractionSource)
}

private const val PRESSED_ALPHA = 0.4f
private const val RELEASED_ALPHA = 1f
private const val RELEASED_TO_PRESSED_MILLIS = 0
private const val PRESSED_TO_RELEASED_MILLIS = 350
