package com.casavirupa.voluntariat.shared.model.payment

import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentLinkTest {

    @Test
    fun `builds the payment url with the name and the amount`() {
        assertEquals(
            "https://casavirupa.com/pagaments?nom=Maria+Puig&quantitat=12.50",
            paymentUrl("Maria Puig", 12.5),
        )
    }

    @Test
    fun `percent encodes accents and keeps spaces as plus`() {
        assertEquals("%C3%92scar+L%C3%B3pez", "Òscar López".formUrlEncode())
        assertEquals("Joan-Pere_1.a", "Joan-Pere_1.a".formUrlEncode())
        assertEquals("a%26b%3Dc%2Fd", "a&b=c/d".formUrlEncode())
    }

    @Test
    fun `trims the name before encoding`() {
        assertEquals(
            "https://casavirupa.com/pagaments?nom=Anna&quantitat=3.00",
            paymentUrl("  Anna ", 3.0),
        )
    }

    @Test
    fun `formats the amount with two decimals and a dot`() {
        assertEquals("3.00", 3.0.toAmountParam())
        assertEquals("0.50", 0.5.toAmountParam())
        assertEquals("12.35", 12.345.toAmountParam())
        assertEquals("120.10", 120.1.toAmountParam())
    }
}
