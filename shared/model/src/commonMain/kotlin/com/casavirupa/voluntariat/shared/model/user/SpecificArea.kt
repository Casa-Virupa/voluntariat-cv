package com.casavirupa.voluntariat.shared.model.user

/**
 * The specific volunteering areas, in the app's Catalan alphabetical order.
 *
 * The Firestore codes (see `toSpecificArea()` / `toFirebaseValue()` in shared/data and
 * `AREA_LABELS` in the dashboard's `lib/contract.ts`) are the stable contract and are kept even
 * when an area is renamed: `technical_and_audiovisual` is now «Audiovisual», `gardening` is
 * «Exteriors i Jardineria», `community_health` is «Salut» and `technical_and_texts` is «Textos».
 */
enum class SpecificArea {
    Amrita,
    Animals,
    Shop,
    Audiovisual,
    Communication,
    VolunteerCoordination,
    Kitchen,
    GraphicalDesign,
    VirupaEditions,
    ExteriorsAndGardening,
    Grove,
    Registrations,
    Labor,
    Maintenance,
    Works,
    Pedagogical,
    Health,
    Grants,
    TechnicalAndProgramming,
    Temple,
    Texts,
    Transcriptions,
    Unknown
}
