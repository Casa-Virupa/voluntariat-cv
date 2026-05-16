package com.casavirupa.voluntariat.shared.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
open class MainNavKey(val showNavigationBar: Boolean = false) : NavKey