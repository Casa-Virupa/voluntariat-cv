package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.TimeZone

/**
 * The time zone every calendar day in the app is read in: Casa Virupa's, not the phone's.
 * Bookings are stored as local midnights, Google Calendar all-day blocks as Madrid midnights
 * and shifts are morning/afternoon at the house, so a volunteer abroad (e.g. in LA) must see
 * the same days as someone in Madrid.
 */
val CasaVirupaTimeZone: TimeZone = TimeZone.of("Europe/Madrid")
