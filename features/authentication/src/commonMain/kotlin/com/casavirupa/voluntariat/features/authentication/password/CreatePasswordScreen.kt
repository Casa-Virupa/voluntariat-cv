package com.casavirupa.voluntariat.features.authentication.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ConfirmPasswordScreen(
    onCreatePassword: () -> Unit,
    viewModel: CreatePasswordViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val newPassword by viewModel.newPassword.collectAsStateWithLifecycle()
    val confirmNewPassword by viewModel.confirmNewPassword.collectAsStateWithLifecycle()

    ConfirmPasswordContent(
        newPassword = newPassword,
        confirmNewPassword = confirmNewPassword,
        onNewPasswordChanged = viewModel::onNewPasswordChanged,
        onConfirmNewPasswordChanged = viewModel::onConfirmNewPasswordChanged,
        showPasswordNotMatchError = uiState.showPasswordNotMatchError,
        onClickCreatePassword = viewModel::onCreatePassword,
    )

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnCreatePassword by rememberUpdatedState(onCreatePassword)
    LaunchedEffect(viewModel, lifecycle) {
        snapshotFlow { viewModel.uiState }
            .filter { it.value.navigateToSchedule }
            .flowWithLifecycle(lifecycle)
            .collect {
                currentOnCreatePassword()
            }
    }
}

@Composable
private fun ConfirmPasswordContent(
    newPassword: String,
    confirmNewPassword: String,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    showPasswordNotMatchError: Boolean,
    onClickCreatePassword: () -> Unit,
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
            isError = showPasswordNotMatchError,
            supportingText = if (showPasswordNotMatchError) {
                { Text("Las contraseñas no coinciden") }
            } else {
                null
            }
        )
        Button(
            onClick = onClickCreatePassword,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Text("Crear contraseña")
        }
    }
}