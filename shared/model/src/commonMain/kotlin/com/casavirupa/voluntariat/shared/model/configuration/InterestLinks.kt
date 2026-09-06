package com.casavirupa.voluntariat.shared.model.configuration

import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate

/**
 * Mirror of the dashboard-published `configuration/links` document.
 *
 * [text] is the general text (users without a volunteer type and older app versions).
 * The per-type texts fall back to [text] when blank — see [textFor].
 */
data class InterestLinks(
    val text: String = "",
    val textHabitual: String = "",
    val textMitra: String = "",
    val textLongStay: String = "",
    val updatedAt: LocalDate? = null,
) {
    /** Text to show a user of the given [type]: the type-specific one if set, else the general one. */
    fun textFor(type: UserVolunteerType?): String {
        val specific = when (type) {
            UserVolunteerType.Habitual -> textHabitual
            UserVolunteerType.Mitra -> textMitra
            UserVolunteerType.LongStay -> textLongStay
            null -> ""
        }
        return if (specific.isNotBlank()) specific else text
    }

    companion object {
        val Empty = InterestLinks()
    }
}
