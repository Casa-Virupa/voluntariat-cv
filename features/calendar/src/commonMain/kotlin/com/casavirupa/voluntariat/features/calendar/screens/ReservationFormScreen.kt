package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.features.calendar.components.ChipMultiOptionsSelector
import com.casavirupa.voluntariat.features.calendar.components.ChipOptionsSelector
import com.casavirupa.voluntariat.features.calendar.viewmodels.ReservationFormViewModel
import com.casavirupa.voluntariat.shared.common.ui.displayName
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.DateTextField
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.SpecificArea
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.afternoon
import voluntariatcv.features.calendar.generated.resources.all_day
import voluntariatcv.features.calendar.generated.resources.confirm
import voluntariatcv.features.calendar.generated.resources.date_placeholder
import voluntariatcv.features.calendar.generated.resources.dinner
import voluntariatcv.features.calendar.generated.resources.ic_calendar_today
import voluntariatcv.features.calendar.generated.resources.ic_close
import voluntariatcv.features.calendar.generated.resources.lunch
import voluntariatcv.features.calendar.generated.resources.meals_included
import voluntariatcv.features.calendar.generated.resources.morning
import voluntariatcv.features.calendar.generated.resources.overnight_stay
import voluntariatcv.features.calendar.generated.resources.reservation_form_title
import voluntariatcv.features.calendar.generated.resources.schedule
import voluntariatcv.features.calendar.generated.resources.select_date
import voluntariatcv.features.calendar.generated.resources.specific_volunteering
import voluntariatcv.features.calendar.generated.resources.stay_to_sleep

@Composable
internal fun ReservationFormScreen(
    onNavBack: () -> Unit,
    viewModel: ReservationFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val sleep by viewModel.sleep.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val technicalArea by viewModel.technicalArea.collectAsStateWithLifecycle()
    val meals by viewModel.meals.collectAsStateWithLifecycle()

    ReservationFormContent(
        onNavBack = onNavBack,
        date = date,
        sleep = sleep,
        schedule = schedule,
        technicalArea = technicalArea,
        meals = meals,
        onDateChanged = viewModel::onDateChanged,
        onSleepChanged = viewModel::onSleepChanged,
        onScheduleChanged = viewModel::onScheduleChanged,
        onTechnicalAreaChanged = viewModel::onTechnicalAreaChanged,
        onMealChanged = viewModel::onMealChanged,
        onConfirm = viewModel::onConfirm,
    )

    val currentOnNavBack by rememberUpdatedState(onNavBack)
    LaunchedEffect(uiState.isFormSavedSuccessfully) {
        if (uiState.isFormSavedSuccessfully) {
            currentOnNavBack()
            viewModel.onNavigationHandled()
        }
    }
}

@Composable
private fun ReservationFormContent(
    onNavBack: () -> Unit,
    date: LocalDate?,
    sleep: Boolean,
    schedule: Shift?,
    technicalArea: SpecificArea?,
    meals: Set<Meal>,
    onDateChanged: (LocalDate) -> Unit,
    onSleepChanged: (Boolean) -> Unit,
    onScheduleChanged: (Shift) -> Unit,
    onTechnicalAreaChanged: (SpecificArea) -> Unit,
    onMealChanged: (Meal) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MediumTopBar(
                title = stringResource(Res.string.reservation_form_title),
                navigationIcon = {
                    IconButton(onClick = onNavBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            ReservationForm(
                date = date,
                sleep = sleep,
                schedule = schedule,
                technicalArea = technicalArea,
                meals = meals,
                onDateChanged = onDateChanged,
                onSleepChanged = onSleepChanged,
                onScheduleChanged = onScheduleChanged,
                onTechnicalAreaChanged = onTechnicalAreaChanged,
                onMealChanged = onMealChanged,
                modifier = Modifier.fillMaxSize(),
            )
            CVButton(
                text = stringResource(Res.string.confirm),
                onClick = onConfirm,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun ReservationForm(
    date: LocalDate?,
    sleep: Boolean,
    schedule: Shift?,
    technicalArea: SpecificArea?,
    meals: Set<Meal>,
    onDateChanged: (LocalDate) -> Unit,
    onSleepChanged: (Boolean) -> Unit,
    onScheduleChanged: (Shift) -> Unit,
    onTechnicalAreaChanged: (SpecificArea) -> Unit,
    onMealChanged: (Meal) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        FormSection(
            title = stringResource(Res.string.select_date),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            DateTextField(
                date = date,
                onDateChanged = onDateChanged,
                placeholder = stringResource(Res.string.date_placeholder),
                pattern = DATE_PATTERN,
                minDate = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date,
            )
        }
        HorizontalDivider()
        FormSection(
            title = stringResource(Res.string.overnight_stay),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            SleepSwitch(
                sleep = sleep,
                onSleepChanged = onSleepChanged,
            )
        }
        HorizontalDivider()
        FormSection(
            title = stringResource(Res.string.schedule),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            ChipOptionsSelector(
                options = Shift.entries.filter { it != Shift.Unknown }.toList(),
                selected = schedule,
                onOptionSelected = onScheduleChanged,
                displayMode = { Text(it.displayName()) },
            )
        }
        HorizontalDivider()
        FormSection(
            title = stringResource(Res.string.specific_volunteering),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            ChipOptionsSelector(
                options = SpecificArea.entries.toList(),
                selected = technicalArea,
                onOptionSelected = onTechnicalAreaChanged,
                displayMode = { Text(it.displayName()) },
            )
        }
        HorizontalDivider()
        FormSection(
            title = stringResource(Res.string.meals_included),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            ChipMultiOptionsSelector(
                options = Meal.entries.filter { it != Meal.Unknown }.toList(),
                selected = meals,
                onOptionToggled = onMealChanged,
                displayMode = { Text(it.displayName()) },
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}


@Composable
private fun FormSection(
    title: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(vertical = 20.dp, horizontal = 16.dp)) {
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        content()
    }
}

@Composable
private fun SleepSwitch(
    sleep: Boolean,
    onSleepChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.stay_to_sleep),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Switch(
            checked = sleep,
            onCheckedChange = onSleepChanged,
        )
    }
}

private const val DATE_PATTERN = "d MMMM yyyy"
