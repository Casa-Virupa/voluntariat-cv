package com.casavirupa.voluntariat.features.authentication.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVOutlinedButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTextField
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.features.authentication.generated.resources.Res
import voluntariatcv.features.authentication.generated.resources.accept
import voluntariatcv.features.authentication.generated.resources.cancel
import voluntariatcv.features.authentication.generated.resources.email_label
import voluntariatcv.features.authentication.generated.resources.email_placeholder
import voluntariatcv.features.authentication.generated.resources.ic_mail
import voluntariatcv.features.authentication.generated.resources.invalid_email_error
import voluntariatcv.features.authentication.generated.resources.reset_password_description
import voluntariatcv.features.authentication.generated.resources.reset_password_error
import voluntariatcv.features.authentication.generated.resources.reset_password_sent_description
import voluntariatcv.features.authentication.generated.resources.reset_password_sent_title
import voluntariatcv.features.authentication.generated.resources.reset_password_title
import voluntariatcv.features.authentication.generated.resources.send
import voluntariatcv.features.authentication.generated.resources.sending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForgotPasswordDialog(
    state: ForgotPasswordUiState,
    onEmailChanged: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSent = state.status == ForgotPasswordStatus.Sent
    val title = if (isSent) {
        stringResource(Res.string.reset_password_sent_title)
    } else {
        stringResource(Res.string.reset_password_title)
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)) {
            DialogHeader(title = title)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isSent) {
                    SentContent(email = state.email.trim(), onAccept = onDismiss)
                } else {
                    RequestContent(
                        state = state,
                        onEmailChanged = onEmailChanged,
                        onSend = onSend,
                        onCancel = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_mail),
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
}

@Composable
private fun RequestContent(
    state: ForgotPasswordUiState,
    onEmailChanged: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val isSending = state.status == ForgotPasswordStatus.Sending
    val errorText = when (state.status) {
        ForgotPasswordStatus.InvalidEmail -> stringResource(Res.string.invalid_email_error)
        ForgotPasswordStatus.Error -> stringResource(Res.string.reset_password_error)
        else -> null
    }

    Text(
        text = stringResource(Res.string.reset_password_description),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
    CVTextField(
        value = state.email,
        onValueChanged = onEmailChanged,
        label = stringResource(Res.string.email_label),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Send,
        ),
        placeholder = stringResource(Res.string.email_placeholder),
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.ic_mail),
                contentDescription = null,
                tint = if (errorText != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        showError = errorText != null,
        supportingText = errorText,
        enabled = !isSending,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CVOutlinedButton(
            text = stringResource(Res.string.cancel),
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        )
        CVButton(
            text = if (isSending) {
                stringResource(Res.string.sending)
            } else {
                stringResource(Res.string.send)
            },
            onClick = { if (!isSending) onSend() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SentContent(
    email: String,
    onAccept: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.reset_password_sent_description, email),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
    )
    CVButton(
        text = stringResource(Res.string.accept),
        onClick = onAccept,
        modifier = Modifier.fillMaxWidth(),
    )
}
