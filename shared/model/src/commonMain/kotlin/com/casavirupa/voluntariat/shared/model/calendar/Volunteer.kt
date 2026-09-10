package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.payment.Prices
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Volunteer(
    val id: VolunteerId,
    val userId: UserId,
    val date: LocalDate,
    val shifts: List<Shift>,
    val meals: List<Meal>,
    val sleep: Boolean,
) {
    fun calculateTotalToPay(prices: Prices = Prices.Default): Double {
        val mealsTotal = meals.sumOf { meal ->
            when (meal) {
                Meal.Breakfast -> prices.breakfast
                Meal.BreakfastNextDay -> prices.breakfastNextDay
                Meal.Lunch -> prices.lunch
                Meal.Dinner -> prices.dinner
                Meal.Unknown -> 0.0
            }
        }
        val sleepTotal = if (sleep) prices.sleep else 0.0
        return mealsTotal + sleepTotal
    }

    fun calculateHours(): Int = shifts.sumOf { it.getHour() }

    fun isOwnedBy(viewer: User): Boolean = userId == viewer.id

    // A volunteer sees their own bookings, general volunteering and specific volunteering in
    // one of their own areas. The coordination team sees everyone.
    fun isVisibleTo(viewer: User): Boolean =
        isOwnedBy(viewer) ||
            viewer.role == UserRole.CoordinationTeam ||
            shifts.any { shift ->
                when (val type = shift.type) {
                    VolunteerType.General -> true
                    is VolunteerType.Specific -> type.specificArea in viewer.specificAreas
                }
            }

    // Meals and overnight stays are private: only the volunteer themselves and the
    // coordination team (who manage the house logistics) can see them.
    fun areServicesVisibleTo(viewer: User): Boolean =
        isOwnedBy(viewer) || viewer.role == UserRole.CoordinationTeam
}

data class VolunteerId(val value: String) {
    companion object {
        val Empty = VolunteerId("")
    }
}

sealed class Shift {
    abstract val type: VolunteerType
    abstract val timeRange: TimeRange
    // Done remotely rather than at the house. Only specific areas that allow it (see
    // AreaConfig) can be online; general volunteering is always on-site.
    abstract val online: Boolean

    data class Morning(
        override val type: VolunteerType,
        override val timeRange: TimeRange,
        override val online: Boolean = false,
    ) : Shift()

    data class Afternoon(
        override val type: VolunteerType,
        override val timeRange: TimeRange,
        override val online: Boolean = false,
    ) : Shift()

    fun getHour(): Int = HALF_JOURNEY

    companion object {
        private const val HALF_JOURNEY = 4
    }
}

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime,
) {
    companion object {
        val DefaultMorning = TimeRange(
            start = LocalTime(10, 0),
            end = LocalTime(14, 0)
        )
        val DefaultAfternoon = TimeRange(
            start = LocalTime(16, 30),
            end = LocalTime(20, 30),
        )
        val SundayMorning = TimeRange(
            start = LocalTime(9, 0),
            end = LocalTime(14, 0),
        )
        val SundayAfternoon = TimeRange(
            start = LocalTime(16, 30),
            end = LocalTime(19, 30),
        )

        fun defaultMorning(isSunday: Boolean) =
            if (isSunday) SundayMorning else DefaultMorning

        fun defaultAfternoon(isSunday: Boolean) =
            if (isSunday) SundayAfternoon else DefaultAfternoon
    }
}

enum class Meal {
    // Breakfast on the volunteering day itself (early arrival, no overnight stay)
    Breakfast,
    // Breakfast the morning after an overnight stay (hotel-style); only with sleep = true
    BreakfastNextDay,
    Lunch,
    Dinner,
    Unknown,
}

sealed class VolunteerType {
    data object General : VolunteerType()

    data class Specific(val specificArea: SpecificArea) : VolunteerType()
}
