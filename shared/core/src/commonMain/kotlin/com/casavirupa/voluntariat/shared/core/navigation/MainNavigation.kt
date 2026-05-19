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
fun rememberMainNavigationState(
    startKey: NavKey,
    config: SavedStateConfiguration,
): MainNavigationState {
    val stack = rememberNavBackStack(config, startKey)
    return remember(startKey) {
        MainNavigationState(stack = stack)
    }
}

class MainNavigationState(val stack: NavBackStack<NavKey>) {
    val currentKey by derivedStateOf { stack.last() as MainNavKey }
}

class MainNavigator(val state: MainNavigationState) {
    fun navigate(key: MainNavKey) {
        if (key.shouldLogOut) {
            state.stack.clear()
        }
        state.stack.add(key)
    }

    fun goBack() {
        state.stack.removeLastOrNull()
    }
}

@Composable
fun MainNavigationState.toEntries(
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