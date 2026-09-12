package com.casavirupa.voluntariat.shared.model.payment

import kotlin.math.roundToLong

private const val PAYMENT_BASE_URL = "https://casavirupa.com/pagaments"

/**
 * Link to the Casa Virupa payment page, pre-filled with the volunteer's name and the amount
 * they owe. The web side reads `nom` and `quantitat` (first to show payment instructions,
 * later to open a pre-filled Stripe checkout) so both keys are part of the contract.
 * The amount always carries two decimals with a dot, regardless of the device locale.
 */
fun paymentUrl(name: String, amount: Double): String =
    "$PAYMENT_BASE_URL?nom=${name.trim().formUrlEncode()}&quantitat=${amount.toAmountParam()}"

internal fun Double.toAmountParam(): String {
    val cents = (this * 100).roundToLong()
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$whole.$fraction"
}

// application/x-www-form-urlencoded: unreserved chars as-is, space as '+', anything else
// percent-encoded byte by byte (UTF-8). The model module has no networking dependency.
internal fun String.formUrlEncode(): String =
    buildString {
        for (byte in this@formUrlEncode.encodeToByteArray()) {
            val char = byte.toInt().toChar()
            when {
                byte.toInt() == ' '.code -> append('+')
                char.isUnreserved() -> append(char)
                else -> {
                    append('%')
                    append(HEX_DIGITS[(byte.toInt() shr 4) and 0x0F])
                    append(HEX_DIGITS[byte.toInt() and 0x0F])
                }
            }
        }
    }

private fun Char.isUnreserved(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this in "-._~"

private const val HEX_DIGITS = "0123456789ABCDEF"
