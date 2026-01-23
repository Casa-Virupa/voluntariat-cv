package com.casavirupa.voluntariat.features.authentication.confirmation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ConfirmPasswordScreen(viewModel: CreatePasswordViewModel = koinViewModel()) {
    val newPassword by viewModel.newPassword.collectAsStateWithLifecycle()
    val confirmNewPassword by viewModel.confirmNewPassword.collectAsStateWithLifecycle()

    ConfirmPasswordContent(
        newPassword = newPassword,
        confirmNewPassword = confirmNewPassword,
        onNewPasswordChanged = viewModel::onNewPasswordChanged,
        onConfirmNewPasswordChanged = viewModel::onConfirmNewPasswordChanged,
    )
}

@Composable
private fun ConfirmPasswordContent(
    newPassword: String,
    confirmNewPassword: String,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Crear nueva contraseña",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        TextField(
            value = newPassword,
            onValueChange = onNewPasswordChanged,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            label = { Text("Correo electrónico") },
        )
        TextField(
            value = confirmNewPassword,
            onValueChange = onConfirmNewPasswordChanged,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            label = { Text("Contraseña") },
        )
    }
}