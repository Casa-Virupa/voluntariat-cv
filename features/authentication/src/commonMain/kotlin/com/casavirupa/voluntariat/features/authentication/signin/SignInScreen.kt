package com.casavirupa.voluntariat.features.authentication.signin

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LogInScreen(
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    viewModel: SignInViewModel = koinViewModel()
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()

    LogInContent(
        email = email,
        password = password,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onClickLogIn = viewModel::onSignIn,
    )

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnNavigateToCreatePassword by rememberUpdatedState(onNavigateToCreatePassword)
    val currentOnNavigateToSchedule by rememberUpdatedState(onNavigateToSchedule)

    LaunchedEffect(viewModel, lifecycle) {
        viewModel.uiState
            .flowWithLifecycle(lifecycle)
            .collect { state ->
                when {
                    state.navigateToCreatePassword -> currentOnNavigateToCreatePassword()
                    state.navigateToSchedule -> currentOnNavigateToSchedule()
                }
            }
    }
}

@Composable
private fun LogInContent(
    email: String,
    password: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onClickLogIn: () -> Unit,
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
            text = "Iniciar sesión en Voluntariat Casa Virupa",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        TextField(
            value = email,
            onValueChange = onEmailChanged,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            label = { Text("Correo electrónico") },
        )
        TextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            label = { Text("Contraseña") },
        )
        Button(
            onClick = onClickLogIn,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Text("Iniciar sesión")
        }
    }
}