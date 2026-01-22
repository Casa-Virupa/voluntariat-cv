package com.casavirupa.voluntariat.features.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LogInScreen(viewModel: LogInViewModel = koinViewModel()) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()

    LogInContent(
        email = email,
        password = password,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onClickLogIn = viewModel::onLogIn,
    )
}

@Composable
private fun LogInContent(
    email: String,
    password: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onClickLogIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextField(
            value = email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
        )
        TextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            label = { Text("Password") },
        )
        Button(
            onClick = onClickLogIn,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
        ) {
            Text("Log In")
        }
    }
}