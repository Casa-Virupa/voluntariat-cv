package com.casavirupa.voluntariat.shared.model.configuration

import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AreaConfigTest {
    private val config = AreaConfig(
        onlineAreas = setOf(SpecificArea.Transcriptions, SpecificArea.Communication),
    )

    @Test
    fun `general volunteering is never online`() {
        assertFalse(config.allowsOnline(VolunteerType.General))
    }

    @Test
    fun `specific area is online only when listed`() {
        assertTrue(config.allowsOnline(VolunteerType.Specific(SpecificArea.Transcriptions)))
        assertFalse(config.allowsOnline(VolunteerType.Specific(SpecificArea.Kitchen)))
        assertTrue(config.allowsOnline(SpecificArea.Communication))
        assertFalse(config.allowsOnline(SpecificArea.Kitchen))
        assertFalse(config.allowsOnline(null))
    }

    @Test
    fun `empty config allows nothing`() {
        assertFalse(AreaConfig.Empty.allowsOnline(VolunteerType.Specific(SpecificArea.Transcriptions)))
        assertEquals(emptyList(), AreaConfig.Empty.onlineAreasOf(user(SpecificArea.Transcriptions)))
    }

    @Test
    fun `online areas of a user keep the user's order and drop the rest`() {
        val user = user(SpecificArea.Kitchen, SpecificArea.Communication, SpecificArea.Transcriptions)
        assertEquals(
            listOf(SpecificArea.Communication, SpecificArea.Transcriptions),
            config.onlineAreasOf(user),
        )
    }

    private fun user(vararg areas: SpecificArea) = User(
        id = UserId("u"),
        name = "Anna",
        email = "anna@example.com",
        role = UserRole.Volunteer(UserVolunteerType.Habitual),
        volunteerType = UserVolunteerType.Habitual,
        hasOnboardingCompleted = true,
        specificAreas = areas.toList(),
        isMember = false,
    )
}
