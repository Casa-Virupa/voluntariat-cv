package com.casavirupa.voluntariat.features.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProfileScreen(
    onNavigateToSignIn: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    ProfileContent(onLogOut = viewModel::logOut)
}

@Composable
private fun ProfileContent(onLogOut: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CVButton(
                text = "Tancar sessió",
                onClick = onLogOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }
    }
}