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
        state.stack.add(key)
    }

    fun goBack() {
        state.stack.removeLastOrNull()
    }

    /**
     * Bottom-bar navigation: shows [key] without stacking a duplicate of it. Already on top →
     * no-op; somewhere below → pop back to it; otherwise push it.
     */
    fun switchTo(key: MainNavKey) {
        val stack = state.stack
        when {
            stack.lastOrNull() == key -> Unit
            key in stack -> popTo(key)
            else -> stack.add(key)
        }
    }

    /**
     * Pops every entry above the most recent [key], so it becomes the visible screen again.
     * If [key] isn't on the stack, everything but the root entry is popped.
     */
    fun popTo(key: MainNavKey) {
        val stack = state.stack
        while (stack.size > 1 && stack.last() != key) {
            stack.removeAt(stack.lastIndex)
        }
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