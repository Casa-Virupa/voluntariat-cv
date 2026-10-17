package com.casavirupa.voluntariat.shared.model.commitment

import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommitmentRulesTest {

    private val anna = UserId("uid-anna")

    private fun rule(
        id: String,
        scope: CommitmentScope,
        targetMinutes: Int,
        periodKind: CommitmentPeriod,
        validFrom: LocalDate = LocalDate(1970, 1, 1),
        validTo: LocalDate? = null,
        area: CommitmentArea = CommitmentArea.Total,
    ) = CommitmentRule(
        id = id,
        scope = scope,
        area = area,
        periodKind = periodKind,
        targetMinutes = targetMinutes,
        validFrom = validFrom,
        validTo = validTo,
    )

    // The worked example from the dashboard's COMMITMENTS-APP.md spec
    private val workedExample = CommitmentRules(
        listOf(
            rule(
                id = "dash-1",
                scope = CommitmentScope.VolunteerType(UserVolunteerType.Habitual),
                targetMinutes = 480,
                periodKind = CommitmentPeriod.Month,
            ),
            rule(
                id = "dash-2",
                scope = CommitmentScope.VolunteerType(UserVolunteerType.Mitra),
                targetMinutes = 3600,
                periodKind = CommitmentPeriod.Quarter,
            ),
            rule(
                id = "dash-9",
                scope = CommitmentScope.User(anna),
                targetMinutes = 240,
                periodKind = CommitmentPeriod.Month,
                validFrom = LocalDate(2026, 9, 1),
            ),
        ),
    )

    @Test
    fun userRuleNotYetValidFallsBackToVolunteerTypeRule() {
        val target = workedExample.targetFor(
            uid = anna,
            volunteerType = UserVolunteerType.Habitual,
            periodStart = LocalDate(2026, 8, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(480, target?.targetMinutes)
        assertFalse(target!!.isScaled)
    }

    @Test
    fun userRuleBeatsVolunteerTypeRuleOnceValid() {
        val target = workedExample.targetFor(
            uid = anna,
            volunteerType = UserVolunteerType.Habitual,
            periodStart = LocalDate(2026, 9, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(240, target?.targetMinutes)
    }

    @Test
    fun quarterRuleViewedMonthlyIsScaledDownAndFlagged() {
        val target = workedExample.targetFor(
            uid = UserId("uid-mitra"),
            volunteerType = UserVolunteerType.Mitra,
            periodStart = LocalDate(2026, 9, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(1200, target?.targetMinutes)
        assertEquals(CommitmentPeriod.Quarter, target?.nativePeriod)
        assertTrue(target!!.isScaled)
    }

    @Test
    fun monthRuleViewedQuarterlyIsScaledUp() {
        val target = workedExample.targetFor(
            uid = anna,
            volunteerType = UserVolunteerType.Habitual,
            periodStart = LocalDate(2026, 7, 1),
            viewedPeriod = CommitmentPeriod.Quarter,
        )

        assertEquals(480 * 3, target?.targetMinutes)
        assertTrue(target!!.isScaled)
    }

    @Test
    fun userWithNoVolunteerTypeMatchesNoTypeRule() {
        val target = workedExample.targetFor(
            uid = UserId("uid-coordinator"),
            volunteerType = null,
            periodStart = LocalDate(2026, 8, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertNull(target)
    }

    @Test
    fun globalRuleAppliesToEveryone() {
        val rules = CommitmentRules(
            listOf(
                rule(
                    id = "dash-3",
                    scope = CommitmentScope.Global,
                    targetMinutes = 120,
                    periodKind = CommitmentPeriod.Month,
                ),
            ),
        )

        val target = rules.targetFor(
            uid = UserId("uid-anyone"),
            volunteerType = null,
            periodStart = LocalDate(2026, 8, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(120, target?.targetMinutes)
    }

    @Test
    fun validToIsExclusive() {
        val rules = CommitmentRules(
            listOf(
                rule(
                    id = "dash-4",
                    scope = CommitmentScope.Global,
                    targetMinutes = 120,
                    periodKind = CommitmentPeriod.Month,
                    validFrom = LocalDate(2026, 1, 1),
                    validTo = LocalDate(2026, 7, 1),
                ),
            ),
        )

        val june = rules.targetFor(
            uid = anna,
            volunteerType = null,
            periodStart = LocalDate(2026, 6, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )
        val july = rules.targetFor(
            uid = anna,
            volunteerType = null,
            periodStart = LocalDate(2026, 7, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(120, june?.targetMinutes)
        assertNull(july)
    }

    @Test
    fun tiesBreakOnGreatestValidFromThenGreatestNumericId() {
        val rules = CommitmentRules(
            listOf(
                rule(
                    id = "dash-5",
                    scope = CommitmentScope.Global,
                    targetMinutes = 100,
                    periodKind = CommitmentPeriod.Month,
                    validFrom = LocalDate(2026, 1, 1),
                ),
                rule(
                    id = "dash-6",
                    scope = CommitmentScope.Global,
                    targetMinutes = 200,
                    periodKind = CommitmentPeriod.Month,
                    validFrom = LocalDate(2026, 3, 1),
                ),
                rule(
                    id = "dash-8",
                    scope = CommitmentScope.Global,
                    targetMinutes = 300,
                    periodKind = CommitmentPeriod.Month,
                    validFrom = LocalDate(2026, 3, 1),
                ),
            ),
        )

        val target = rules.targetFor(
            uid = anna,
            volunteerType = null,
            periodStart = LocalDate(2026, 8, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(300, target?.targetMinutes)
    }

    @Test
    fun rulesOfOtherAreasAreIgnored() {
        val rules = CommitmentRules(
            listOf(
                rule(
                    id = "dash-10",
                    scope = CommitmentScope.Global,
                    targetMinutes = 120,
                    periodKind = CommitmentPeriod.Month,
                    area = CommitmentArea.Specific(SpecificArea.Kitchen),
                ),
            ),
        )

        val target = rules.targetFor(
            uid = anna,
            volunteerType = null,
            periodStart = LocalDate(2026, 8, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertNull(target)
    }

    @Test
    fun areaTargetsResolvePerAreaAndScaleToViewedPeriod() {
        // A mitra with a 60 h/quarter total plus a personal 8 h/month area rule,
        // viewed in their native quarter view
        val oscar = UserId("uid-oscar")
        val rules = CommitmentRules(
            listOf(
                rule(
                    id = "dash-1",
                    scope = CommitmentScope.VolunteerType(UserVolunteerType.Mitra),
                    targetMinutes = 3600,
                    periodKind = CommitmentPeriod.Quarter,
                ),
                rule(
                    id = "dash-4",
                    scope = CommitmentScope.User(oscar),
                    targetMinutes = 480,
                    periodKind = CommitmentPeriod.Month,
                    validFrom = LocalDate(2026, 7, 1),
                    area = CommitmentArea.Specific(SpecificArea.Texts),
                ),
                rule(
                    id = "dash-5",
                    scope = CommitmentScope.User(UserId("uid-franc")),
                    targetMinutes = 240,
                    periodKind = CommitmentPeriod.Month,
                    validFrom = LocalDate(2026, 7, 1),
                    area = CommitmentArea.Specific(SpecificArea.VolunteerCoordination),
                ),
            ),
        )

        val targets = rules.areaTargetsFor(
            uid = oscar,
            volunteerType = UserVolunteerType.Mitra,
            periodStart = LocalDate(2026, 7, 1),
            viewedPeriod = CommitmentPeriod.Quarter,
        )

        // Total rules are excluded, other users' area rules don't leak in
        assertEquals(1, targets.size)
        val techAndTexts = targets[CommitmentArea.Specific(SpecificArea.Texts)]
        assertEquals(480 * 3, techAndTexts?.targetMinutes)
        assertTrue(techAndTexts!!.isScaled)
    }

    @Test
    fun generalAreaResolvesSeparatelyFromTotal() {
        val rules = CommitmentRules(
            listOf(
                rule(
                    id = "dash-1",
                    scope = CommitmentScope.Global,
                    targetMinutes = 480,
                    periodKind = CommitmentPeriod.Month,
                ),
                rule(
                    id = "dash-2",
                    scope = CommitmentScope.Global,
                    targetMinutes = 240,
                    periodKind = CommitmentPeriod.Month,
                    area = CommitmentArea.General,
                ),
            ),
        )

        val targets = rules.areaTargetsFor(
            uid = anna,
            volunteerType = null,
            periodStart = LocalDate(2026, 8, 1),
            viewedPeriod = CommitmentPeriod.Month,
        )

        assertEquals(mapOf<CommitmentArea, Int>(CommitmentArea.General to 240), targets.mapValues { it.value.targetMinutes })
    }
}
