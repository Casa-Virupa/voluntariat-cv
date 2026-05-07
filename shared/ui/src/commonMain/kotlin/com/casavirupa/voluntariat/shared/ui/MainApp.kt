package com.casavirupa.voluntariat.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.casavirupa.voluntariat.features.calendar.navigation.CalendarNavKey
import com.casavirupa.voluntariat.features.calendar.navigation.DayDetailNavKey
import com.casavirupa.voluntariat.features.calendar.navigation.ReservationFormNavKey
import com.casavirupa.voluntariat.features.calendar.navigation.calendarEntry
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import com.casavirupa.voluntariat.shared.core.navigation.rememberMainNavigationState
import com.casavirupa.voluntariat.shared.core.navigation.toEntries
import com.casavirupa.voluntariat.shared.designsystem.theme.VoluntariatCVTheme
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun MainApp() {
    val navigationState = rememberMainNavigationState(
        startKey = CalendarNavKey,
        config = mainContentNavigationConfig(),
    )
    val navigator = remember { MainNavigator(navigationState) }
    val entryProvider = entryProvider {
        calendarEntry(navigator)
    }

    VoluntariatCVTheme {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = navigationState.currentKey.showNavigationBar,
                ) {
                    BottomNavigationBar()
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = navigator::goBack,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                    ),
            )
        }
    }
}

private fun mainContentNavigationConfig() = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(CalendarNavKey::class, CalendarNavKey.serializer())
            subclass(ReservationFormNavKey::class, ReservationFormNavKey.serializer())
            subclass(DayDetailNavKey::class, DayDetailNavKey.serializer())
        }
    }
}

@Composable
private fun BottomNavigationBar(
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {

    }
}