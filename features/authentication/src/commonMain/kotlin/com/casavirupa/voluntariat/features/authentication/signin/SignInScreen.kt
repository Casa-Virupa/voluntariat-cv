package com.casavirupa.voluntariat.features.authentication.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTextField
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.authentication.generated.resources.Res
import voluntariatcv.features.authentication.generated.resources.cv_logo
import voluntariatcv.features.authentication.generated.resources.ic_arrow_right
import voluntariatcv.features.authentication.generated.resources.ic_lock
import voluntariatcv.features.authentication.generated.resources.ic_mail
import voluntariatcv.features.authentication.generated.resources.ic_visibility
import voluntariatcv.features.authentication.generated.resources.ic_visibility_off

@Composable
internal fun SignInScreen(
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    viewModel: SignInViewModel = koinViewModel()
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()

    SignInContent(
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
private fun SignInContent(
    email: String,
    password: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onClickLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeaderImageWithTitle()
        SignInInputs(
            email = email,
            password = password,
            onEmailChanged = onEmailChanged,
            onPasswordChanged = onPasswordChanged,
            showPassword = showPassword,
            onTogglePasswordVisibility = { showPassword = !showPassword },
            modifier = Modifier.padding(top = 32.dp),
        )
        CVButton(
            text = "Iniciar sessió",
            onClick = onClickLogIn,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            icon = painterResource(Res.drawable.ic_arrow_right),
        )
    }
}

@Composable
private fun HeaderImageWithTitle(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Image(
            painter = painterResource(Res.drawable.cv_logo),
            contentDescription = null,
            modifier = Modifier
                .height(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = "Voluntariat\nCasa Virupa",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SignInInputs(
    email: String,
    password: String,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualTransformation = if (showPassword) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    Column(modifier = modifier) {
        CVTextField(
            value = email,
            onValueChanged = onEmailChanged,
            label = "Correu electrónic",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            placeholder = "nom@exemple.com",
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_mail),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
        CVTextField(
            value = password,
            onValueChanged = onPasswordChanged,
            label = "Contrasenya",
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            leadingIcon = {
                Icon(
                    painter = painterResource(Res.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            placeholder = "********",
            visualTransformation = visualTransformation,
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    val iconPainter = if (showPassword) {
                        painterResource(Res.drawable.ic_visibility_off)
                    } else {
                        painterResource(Res.drawable.ic_visibility)
                    }
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        )
    }
}