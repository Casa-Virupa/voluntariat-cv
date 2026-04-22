package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.features.calendar.navigation.DayDetailNavKey
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.user.VolunteerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class DayDetailViewModel(
    navKey: DayDetailNavKey,
) : ViewModel() {
    private val date = navKey.date

    private val _uiState = MutableStateFlow(
        DayDetailUiState(headerUi = DetailHeaderUi(date = date.format("dd MMMM"))),
    )
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    headerUi = it.headerUi.copy(numOfVolunteers = 3),
                    dayShifts = DayShifts(
                        allDayVolunteers = listOf(
                            VolunteerItemUi(
                                name = "Nom Cognom",
                                type = VolunteerType.General,
                                meals = listOf(Meal.Breakfast, Meal.Lunch, Meal.Dinner),
                                sleep = false,
                            )
                        ),
                        morningVolunteers = listOf(
                            VolunteerItemUi(
                                name = "Nom Cognom",
                                type = VolunteerType.General,
                                meals = listOf(Meal.Breakfast, Meal.Lunch, Meal.Dinner),
                                sleep = true,
                            )
                        ),
                        afternoonVolunteers = listOf(
                            VolunteerItemUi(
                                name = "Nom Cognom",
                                type = VolunteerType.General,
                                meals = listOf(Meal.Breakfast, Meal.Lunch, Meal.Dinner),
                                sleep = false,
                            )
                        ),
                    )
                )
            }
        }
    }
}

data class DayDetailUiState(
    val headerUi: DetailHeaderUi,
    val dayShifts: DayShifts = DayShifts(),
)

data class DetailHeaderUi(
    val date: String = "",
    val numOfVolunteers: Int = 0,
)

data class DayShifts(
    val allDayVolunteers: List<VolunteerItemUi> = emptyList(),
    val morningVolunteers: List<VolunteerItemUi> = emptyList(),
    val afternoonVolunteers: List<VolunteerItemUi> = emptyList(),
)

data class VolunteerItemUi(
    val name: String,
    val type: VolunteerType,
    val meals: List<Meal>,
    val sleep: Boolean,
)
