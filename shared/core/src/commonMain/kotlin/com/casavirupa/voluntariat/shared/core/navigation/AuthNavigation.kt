package com.casavirupa.voluntariat.shared.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable

@Composable
fun rememberAuthNavigationState(
    startKey: NavKey,
    config: SavedStateConfiguration,
): AuthNavigationState {
    val stack = rememberNavBackStack(config, startKey)
    return remember(startKey) {
        AuthNavigationState(stack = stack)
    }
}

@Serializable
open class AuthNavKey(val isLastNavKey: Boolean = false) : NavKey

class AuthNavigationState(val stack: NavBackStack<NavKey>) {
    val currentKey by derivedStateOf { stack.last() }
}

class AuthNavigator(val state: AuthNavigationState) {
    fun navigate(key: AuthNavKey) {
        if (key.isLastNavKey) {
            state.stack.clear()
        }
        state.stack.add(key)
    }

    fun goBack() {
        if (state.stack.size > 1) {
            state.stack.removeLastOrNull()
        }
    }
}

@Composable
fun AuthNavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
        rememberViewModelStoreNavEntryDecorator(),
    )
    return rememberDecoratedNavEntries(
        backStack = stack,
        entryDecorators = decorators,
        entryProvider = entryProvider,
    ).toMutableStateList()
}
