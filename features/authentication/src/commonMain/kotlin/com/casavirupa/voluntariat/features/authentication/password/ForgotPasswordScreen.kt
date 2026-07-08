package com.casavirupa.voluntariat.features.authentication.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTextField
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.authentication.generated.resources.Res
import voluntariatcv.features.authentication.generated.resources.create_new_password_title
import voluntariatcv.features.authentication.generated.resources.create_password_description
import voluntariatcv.features.authentication.generated.resources.email_label
import voluntariatcv.features.authentication.generated.resources.email_placeholder
import voluntariatcv.features.authentication.generated.resources.ic_arrow_right
import voluntariatcv.features.authentication.generated.resources.ic_mail

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val email by viewModel.email.collectAsStateWithLifecycle()

    ForgotPasswordContent(
        email = email,
        onEmailChanged = viewModel::onEmailChanged,
        onClickReset = viewModel::resetPassword,
    )
}

@Composable
private fun ForgotPasswordContent(
    email: String,
    onEmailChanged: (String) -> Unit,
    onClickReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Header(modifier = Modifier.padding(bottom = 24.dp))
        CVTextField(
            value = email,
            onValueChanged = onEmailChanged,
            label = stringResource(Res.string.email_label),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            placeholder = stringResource(Res.string.email_placeholder),
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_mail),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
        CVButton(
            text = "Restaurar contrasenya",
            onClick = onClickReset,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            icon = painterResource(Res.drawable.ic_arrow_right),
        )
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.create_new_password_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(Res.string.create_password_description),
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}