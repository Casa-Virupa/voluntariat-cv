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

    // The meals and night to show on the card of `shifts[shiftIndex]` in a day's detail, so a
    // booking with several cards shows each service once (issue #113): breakfast and lunch on
    // the first morning card, dinner and the night (with its next-day breakfast) on the first
    // afternoon card. Without a shift on one side, its services move to the other side's first
    // card so nothing is hidden.
    fun servicesOnCard(shiftIndex: Int): CardServices {
        val firstMorning = firstShiftIndex<Shift.Morning>()
        val firstAfternoon = firstShiftIndex<Shift.Afternoon>()
        val dayCard = firstMorning ?: firstAfternoon
        val nightCard = firstAfternoon ?: firstMorning
        return CardServices(
            meals = meals.filter { meal ->
                shiftIndex == if (meal.isNightSide()) nightCard else dayCard
            },
            sleep = sleep && shiftIndex == nightCard,
        )
    }

    private inline fun <reified T : Shift> firstShiftIndex(): Int? =
        shifts.indices
            .filter { shifts[it] is T }
            .minByOrNull { shifts[it].timeRange.start }

    private fun Meal.isNightSide() = this == Meal.Dinner || this == Meal.BreakfastNextDay
}

data class CardServices(
    val meals: List<Meal>,
    val sleep: Boolean,
)

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
    // Half-open ranges: a shift ending at 18:30 doesn't overlap one starting at 18:30, and an
    // empty or inverted range overlaps nothing. Two shifts of one booking may share a morning
    // or an afternoon as long as they don't overlap (issue #113).
    fun overlaps(other: TimeRange): Boolean =
        isNotEmpty() && other.isNotEmpty() && start < other.end && other.start < end

    private fun isNotEmpty() = start < end

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
