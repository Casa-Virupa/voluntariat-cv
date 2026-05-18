package com.casavirupa.voluntariat.features.authentication.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTextField
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.authentication.generated.resources.Res
import voluntariatcv.features.authentication.generated.resources.actual_password_label
import voluntariatcv.features.authentication.generated.resources.confirm_password_label
import voluntariatcv.features.authentication.generated.resources.create_new_password_title
import voluntariatcv.features.authentication.generated.resources.create_password
import voluntariatcv.features.authentication.generated.resources.create_password_description
import voluntariatcv.features.authentication.generated.resources.ic_arrow_right
import voluntariatcv.features.authentication.generated.resources.ic_lock
import voluntariatcv.features.authentication.generated.resources.ic_visibility
import voluntariatcv.features.authentication.generated.resources.ic_visibility_off
import voluntariatcv.features.authentication.generated.resources.new_password_label
import voluntariatcv.features.authentication.generated.resources.password_not_valid_error
import voluntariatcv.features.authentication.generated.resources.password_placeholder
import voluntariatcv.features.authentication.generated.resources.passwords_do_not_match_error
import voluntariatcv.features.authentication.generated.resources.requirement_8_characters
import voluntariatcv.features.authentication.generated.resources.requirement_number_symbol
import voluntariatcv.features.authentication.generated.resources.requirement_uppercase_lowercase
import voluntariatcv.features.authentication.generated.resources.security_requirements

@Composable
internal fun CreatePasswordScreen(
    onNavigateToSchedule: () -> Unit,
    viewModel: CreatePasswordViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actualPassword by viewModel.actualPassword.collectAsStateWithLifecycle()
    val newPassword by viewModel.newPassword.collectAsStateWithLifecycle()
    val confirmNewPassword by viewModel.confirmNewPassword.collectAsStateWithLifecycle()

    ConfirmPasswordContent(
        actualPassword = actualPassword,
        newPassword = newPassword,
        confirmNewPassword = confirmNewPassword,
        onActualPasswordChanged = viewModel::onActualPasswordChanged,
        onNewPasswordChanged = viewModel::onNewPasswordChanged,
        onConfirmNewPasswordChanged = viewModel::onConfirmNewPasswordChanged,
        showPasswordNotMatchError = uiState.showPasswordNotMatchError,
        showPasswordNotValidError = uiState.showPasswordNotValidError,
        onClickCreatePassword = viewModel::onCreatePassword,
    )

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnNavigateToSchedule by rememberUpdatedState(onNavigateToSchedule)
    LaunchedEffect(viewModel, lifecycle) {
        viewModel.uiState
            .filter { it.navigateToSchedule }
            .flowWithLifecycle(lifecycle)
            .collect {
                currentOnNavigateToSchedule()
            }
    }
}

@Composable
private fun ConfirmPasswordContent(
    actualPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    onActualPasswordChanged: (String) -> Unit,
    onNewPasswordChanged: (String) -> Unit,
    onConfirmNewPasswordChanged: (String) -> Unit,
    showPasswordNotMatchError: Boolean,
    showPasswordNotValidError: Boolean,
    onClickCreatePassword: () -> Unit,
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
        CreatePasswordInputs(
            actualPassword = actualPassword,
            onActualPasswordChanged = onActualPasswordChanged,
            newPassword = newPassword,
            onNewPasswordChanged = onNewPasswordChanged,
            confirmPassword = confirmNewPassword,
            onConfirmPasswordChanged = onConfirmNewPasswordChanged,
            showPasswordNotMatchError = showPasswordNotMatchError,
            showPasswordNotValidError = showPasswordNotValidError,
        )
        SecurityRequirements(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .padding(start = 4.dp),
        )
        CVButton(
            text = stringResource(Res.string.create_password),
            onClick = onClickCreatePassword,
            modifier = Modifier
                .padding(top = 8.dp)
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

@Composable
private fun CreatePasswordInputs(
    actualPassword: String,
    onActualPasswordChanged: (String) -> Unit,
    newPassword: String,
    onNewPasswordChanged: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChanged: (String) -> Unit,
    showPasswordNotMatchError: Boolean,
    showPasswordNotValidError: Boolean,
    modifier: Modifier = Modifier,
) {
    var showActualPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val actualPasswordVisualTransformation = if (showActualPassword) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }
    val newPasswordVisualTransformation = if (showNewPassword) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }
    val confirmPasswordVisualTransformation = if (showConfirmPassword) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    Column(modifier = modifier) {
        CVTextField(
            value = actualPassword,
            onValueChanged = onActualPasswordChanged,
            label = stringResource(Res.string.actual_password_label),
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            leadingIcon = { LeadingIcon() },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            placeholder = stringResource(Res.string.password_placeholder),
            visualTransformation = actualPasswordVisualTransformation,
            trailingIcon = {
                PasswordVisibilityButton(
                    showPassword = showActualPassword,
                    onShowPasswordChanged = { showActualPassword = it },
                )
            }
        )
        CVTextField(
            value = newPassword,
            onValueChanged = onNewPasswordChanged,
            label = stringResource(Res.string.new_password_label),
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            leadingIcon = { LeadingIcon(showError = showPasswordNotValidError) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            placeholder = stringResource(Res.string.password_placeholder),
            visualTransformation = newPasswordVisualTransformation,
            trailingIcon = {
                PasswordVisibilityButton(
                    showPassword = showNewPassword,
                    onShowPasswordChanged = { showNewPassword = it },
                )
            },
            showError = showPasswordNotValidError,
            supportingText = if (showPasswordNotValidError) {
                stringResource(Res.string.password_not_valid_error)
            } else {
                null
            },
        )
        CVTextField(
            value = confirmPassword,
            onValueChanged = onConfirmPasswordChanged,
            label = stringResource(Res.string.confirm_password_label),
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            leadingIcon = { LeadingIcon(showError = showPasswordNotMatchError) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            placeholder = stringResource(Res.string.password_placeholder),
            visualTransformation = confirmPasswordVisualTransformation,
            trailingIcon = {
                PasswordVisibilityButton(
                    showPassword = showConfirmPassword,
                    onShowPasswordChanged = { showConfirmPassword = it },
                )
            },
            showError = showPasswordNotMatchError,
            supportingText = if (showPasswordNotMatchError) {
                stringResource(Res.string.passwords_do_not_match_error)
            } else {
                null
            }
        )
    }
}

@Composable
private fun SecurityRequirements(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.security_requirements).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            RequirementCheck(
                text = stringResource(Res.string.requirement_8_characters),
                modifier = Modifier.padding(top = 8.dp)
            )
            RequirementCheck(
                text = stringResource(Res.string.requirement_uppercase_lowercase),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            RequirementCheck(
                text = stringResource(Res.string.requirement_number_symbol),
            )
        }
    }
}

@Composable
private fun RequirementCheck(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun LeadingIcon(
    modifier: Modifier = Modifier,
    showError: Boolean = false,
) {
    val iconTint = if (showError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Icon(
        painter = painterResource(Res.drawable.ic_lock),
        contentDescription = null,
        modifier = modifier,
        tint = iconTint,
    )
}

@Composable
private fun PasswordVisibilityButton(
    showPassword: Boolean,
    onShowPasswordChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { onShowPasswordChanged(!showPassword) },
        modifier = modifier,
    ) {
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