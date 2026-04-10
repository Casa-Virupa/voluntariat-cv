package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import com.casavirupa.voluntariat.features.calendar.navigation.DayDetailNavKey
import kotlinx.datetime.LocalDate

class DayDetailViewModel(
    private val navKey: DayDetailNavKey,
) : ViewModel() {
    val date: LocalDate = navKey.date
}
