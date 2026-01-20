package com.casavirupa.voluntariat.shared.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.casavirupa.voluntariat.shared.core.BuildEnvironment
import org.koin.compose.koinInject

@Composable
fun App() {
    val env = koinInject<BuildEnvironment>()
    Text("Hello World: ${env.name}")
}