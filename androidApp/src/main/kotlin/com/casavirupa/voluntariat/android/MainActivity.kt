package com.casavirupa.voluntariat.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.casavirupa.voluntariat.shared.common.AppViewModel
import com.casavirupa.voluntariat.shared.common.InitialUserState
import com.casavirupa.voluntariat.shared.ui.RootApp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        var userState by mutableStateOf<InitialUserState?>(null)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appViewModel
                    .initialUserState
                    .onEach { userState = it }
                    .collect()
            }
        }

        splashScreen.setKeepOnScreenCondition {
            userState == null
        }

        setContent {
            userState?.let {
                RootApp(userState = it)
            }
        }
    }
}