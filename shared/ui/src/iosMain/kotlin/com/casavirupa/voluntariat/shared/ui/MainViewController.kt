package com.casavirupa.voluntariat.shared.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.shared.common.AppViewModel
import org.koin.compose.koinInject

fun MainViewController() = ComposeUIViewController {
    val appViewModel: AppViewModel = koinInject()
    val initialUserState by appViewModel.initialUserState.collectAsStateWithLifecycle()

    when (val userState = initialUserState) {
        null -> IOSSplashScreen()
        else -> App(userState = userState)
    }
}

@Composable
private fun IOSSplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Splash screen")
    }
}