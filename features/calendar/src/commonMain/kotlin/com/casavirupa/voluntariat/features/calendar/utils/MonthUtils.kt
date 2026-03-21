package com.casavirupa.voluntariat.features.calendar.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.Month
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.april
import voluntariatcv.features.calendar.generated.resources.august
import voluntariatcv.features.calendar.generated.resources.december
import voluntariatcv.features.calendar.generated.resources.february
import voluntariatcv.features.calendar.generated.resources.january
import voluntariatcv.features.calendar.generated.resources.july
import voluntariatcv.features.calendar.generated.resources.june
import voluntariatcv.features.calendar.generated.resources.march
import voluntariatcv.features.calendar.generated.resources.may
import voluntariatcv.features.calendar.generated.resources.november
import voluntariatcv.features.calendar.generated.resources.october
import voluntariatcv.features.calendar.generated.resources.september

@Composable
fun Month.getName() =
    when (number) {
        1 -> stringResource(Res.string.january)
        2 -> stringResource(Res.string.february)
        3 -> stringResource(Res.string.march)
        4 -> stringResource(Res.string.april)
        5 -> stringResource(Res.string.may)
        6 -> stringResource(Res.string.june)
        7 -> stringResource(Res.string.july)
        8 -> stringResource(Res.string.august)
        9 -> stringResource(Res.string.september)
        10 -> stringResource(Res.string.october)
        11 -> stringResource(Res.string.november)
        12 -> stringResource(Res.string.december)
        else -> ""
    }