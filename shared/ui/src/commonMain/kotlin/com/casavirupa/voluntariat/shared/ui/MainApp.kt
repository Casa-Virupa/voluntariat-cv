package com.casavirupa.voluntariat.shared.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.casavirupa.voluntariat.features.schedule.navigation.ScheduleNavKey
import com.casavirupa.voluntariat.features.schedule.navigation.scheduleEntry
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import com.casavirupa.voluntariat.shared.core.navigation.rememberMainNavigationState
import com.casavirupa.voluntariat.shared.core.navigation.toEntries
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun MainApp() {
    val navigationState = rememberMainNavigationState(
        startKey = ScheduleNavKey,
        config = mainContentNavigationConfig(),
    )
    val navigator = remember { MainNavigator(navigationState) }
    val entryProvider = entryProvider {
        scheduleEntry(navigator)
    }

    Scaffold { innerPadding ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = navigator::goBack,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun mainContentNavigationConfig() = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(ScheduleNavKey::class, ScheduleNavKey.serializer())
        }
    }
}