package com.casavirupa.voluntariat.shared.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MainAppContentNavKey : AuthNavKey(isLastNavKey = true)

@Serializable
open class MainNavKey(
    val showNavigationBar: Boolean = false,
    val shouldLogOut: Boolean = false,
) : NavKey

@Serializable
data object AuthRootNavKey : MainNavKey(shouldLogOut = true)