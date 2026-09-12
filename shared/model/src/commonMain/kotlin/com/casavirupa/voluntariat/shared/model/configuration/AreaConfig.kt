package com.casavirupa.voluntariat.shared.model.configuration

import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User

/**
 * Mirror of the dashboard-published `configuration/areas` document.
 *
 * [onlineAreas] are the specific areas whose volunteering may be done remotely. When the
 * document is missing or can't be read the repository emits [Empty], so no area allows
 * online work and the app never offers the toggle — the safe default.
 */
data class AreaConfig(
    val onlineAreas: Set<SpecificArea> = emptySet(),
) {
    /** General volunteering is always on-site; a specific shift may be online if its area allows it. */
    fun allowsOnline(type: VolunteerType): Boolean =
        when (type) {
            VolunteerType.General -> false
            is VolunteerType.Specific -> type.specificArea in onlineAreas
        }

    fun allowsOnline(area: SpecificArea?): Boolean = area != null && area in onlineAreas

    /** The user's own areas that can be worked online, in the user's order. */
    fun onlineAreasOf(user: User): List<SpecificArea> =
        user.specificAreas.filter { it in onlineAreas }

    companion object {
        val Empty = AreaConfig()
    }
}
