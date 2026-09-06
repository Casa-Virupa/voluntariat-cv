package com.casavirupa.voluntariat.shared.model.configuration

import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlin.test.Test
import kotlin.test.assertEquals

class InterestLinksTest {
    private val links = InterestLinks(
        text = "general",
        textHabitual = "habitual",
        textMitra = "mitra",
        textLongStay = "long stay",
    )

    @Test
    fun `returns the type-specific text when set`() {
        assertEquals("habitual", links.textFor(UserVolunteerType.Habitual))
        assertEquals("mitra", links.textFor(UserVolunteerType.Mitra))
        assertEquals("long stay", links.textFor(UserVolunteerType.LongStay))
    }

    @Test
    fun `users without a volunteer type get the general text`() {
        assertEquals("general", links.textFor(null))
    }

    @Test
    fun `falls back to the general text when the type-specific one is blank`() {
        val partial = links.copy(textMitra = "", textLongStay = "   ")
        assertEquals("general", partial.textFor(UserVolunteerType.Mitra))
        assertEquals("general", partial.textFor(UserVolunteerType.LongStay))
        assertEquals("habitual", partial.textFor(UserVolunteerType.Habitual))
    }

    @Test
    fun `documents from before the per-type fields only carry the general text`() {
        val legacy = InterestLinks(text = "general")
        assertEquals("general", legacy.textFor(UserVolunteerType.Mitra))
        assertEquals("general", legacy.textFor(null))
    }

    @Test
    fun `an empty document yields an empty text for everyone`() {
        assertEquals("", InterestLinks.Empty.textFor(UserVolunteerType.Habitual))
        assertEquals("", InterestLinks.Empty.textFor(null))
    }
}
