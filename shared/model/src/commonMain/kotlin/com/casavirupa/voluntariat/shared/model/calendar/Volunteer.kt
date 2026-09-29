package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.payment.Prices
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

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

    fun calculateMinutes(): Int = shifts.sumOf { it.minutes() }

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

    // Minutes actually booked, from the shift's own time range (volunteers can shorten a
    // shift). Mirrors the dashboard's `shiftMinutes` (lib/dates.ts): an empty or implausibly
    // long range counts as 0 rather than inflating someone's total.
    fun minutes(): Int {
        val seconds = abs(timeRange.end.toSecondOfDay() - timeRange.start.toSecondOfDay())
        val minutes = (seconds / SECONDS_PER_MINUTE.toDouble()).roundToInt()
        return if (minutes == 0 || minutes > MAX_PLAUSIBLE_SHIFT_MINUTES) 0 else minutes
    }

    companion object {
        private const val SECONDS_PER_MINUTE = 60
        private const val MAX_PLAUSIBLE_SHIFT_MINUTES = 720
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
