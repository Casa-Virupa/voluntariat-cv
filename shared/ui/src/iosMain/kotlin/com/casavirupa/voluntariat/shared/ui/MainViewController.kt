package com.casavirupa.voluntariat.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.shared.common.AppViewModel
import com.casavirupa.voluntariat.shared.designsystem.theme.VoluntariatCVTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import voluntariatcv.shared.ui.generated.resources.Res
import voluntariatcv.shared.ui.generated.resources.cv_logo

fun MainViewController() = ComposeUIViewController {
    val appViewModel: AppViewModel = koinInject()
    val initialUserState by appViewModel.initialUserState.collectAsStateWithLifecycle()

    when (val userState = initialUserState) {
        null -> IOSSplashScreen()
        else -> RootApp(userState = userState)
    }
}

@Composable
private fun IOSSplashScreen() {
    VoluntariatCVTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.cv_logo),
                contentDescription = null,
            )
        }
    }
}