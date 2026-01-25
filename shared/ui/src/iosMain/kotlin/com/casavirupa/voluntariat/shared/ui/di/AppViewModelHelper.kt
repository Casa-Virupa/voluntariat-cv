package com.casavirupa.voluntariat.shared.ui.di

import com.casavirupa.voluntariat.shared.common.AppViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppViewModelHelper : KoinComponent {
    private val viewModel: AppViewModel by inject()

    fun getViewModel(): AppViewModel = viewModel
}