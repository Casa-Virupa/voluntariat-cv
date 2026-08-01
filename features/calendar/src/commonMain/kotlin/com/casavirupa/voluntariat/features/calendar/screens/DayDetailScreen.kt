package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailUiState
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayShifts
import com.casavirupa.voluntariat.features.calendar.viewmodels.VolunteerItemUi
import com.casavirupa.voluntariat.features.calendar.viewmodels.VolunteerTypeUi
import com.casavirupa.voluntariat.shared.common.ui.displayName
import com.casavirupa.voluntariat.shared.common.ui.getBackgroundColor
import com.casavirupa.voluntariat.shared.common.ui.getIcon
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.designsystem.components.CVTag
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.designsystem.components.WarningDialog
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.afternoon_shift_tag
import voluntariatcv.features.calendar.generated.resources.all_day
import voluntariatcv.features.calendar.generated.resources.delete
import voluntariatcv.features.calendar.generated.resources.delete_volunteering_description
import voluntariatcv.features.calendar.generated.resources.delete_volunteering_title
import voluntariatcv.features.calendar.generated.resources.ic_afternoon
import voluntariatcv.features.calendar.generated.resources.ic_close
import voluntariatcv.features.calendar.generated.resources.ic_delete
import voluntariatcv.features.calendar.generated.resources.ic_sleep_bed
import voluntariatcv.features.calendar.generated.resources.ic_sun
import voluntariatcv.features.calendar.generated.resources.morning_shift_tag
import voluntariatcv.features.calendar.generated.resources.overnight_stay
import voluntariatcv.features.calendar.generated.resources.shift_afternoon
import voluntariatcv.features.calendar.generated.resources.shift_afternoon_sunday_with_puja
import voluntariatcv.features.calendar.generated.resources.shift_afternoon_sunday_without_puja
import voluntariatcv.features.calendar.generated.resources.shift_morning
import voluntariatcv.features.calendar.generated.resources.shift_morning_sunday
import voluntariatcv.features.calendar.generated.resources.special_activities
import voluntariatcv.features.calendar.generated.resources.volunteers_count

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    onNavBack: () -> Unit,
    viewModel: DayDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val thereIsProtectors by viewModel.thereIsProtectors.collectAsStateWithLifecycle()

    DayDetailContent(
        uiState = uiState,
        events = events,
        onClickBack = onNavBack,
        onDeleteVolunteer = viewModel::onDeleteVolunteer,
        isSunday = viewModel.isSunday,
        thereIsProtectors = thereIsProtectors,
        modifier = modifier,
    )

    if (showDeleteDialog) {
        WarningDialog(
            onDismiss = viewModel::onCloseDeleteDialog,
            title = stringResource(Res.string.delete_volunteering_title),
            description = stringResource(Res.string.delete_volunteering_description),
            onCancel = viewModel::onCloseDeleteDialog,
            confirmText = stringResource(Res.string.delete),
            onConfirm = viewModel::deleteVolunteer,
        )
    }
}

@Composable
private fun DayDetailContent(
    uiState: DayDetailUiState,
    events: List<GoogleCalendarEvent>,
    onClickBack: () -> Unit,
    onDeleteVolunteer: (VolunteerId) -> Unit,
    isSunday: Boolean,
    thereIsProtectors: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MediumTopBar(
                title = uiState.headerUi.date,
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = null,
                        )
                    }
                },
                subtitle = pluralStringResource(
                    Res.plurals.volunteers_count,
                    uiState.headerUi.numOfVolunteers,
                    uiState.headerUi.numOfVolunteers,
                ).uppercase(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        DayShiftsAndEvents(
            dayShifts = uiState.dayShifts,
            events = events,
            onDeleteVolunteer = onDeleteVolunteer,
            isSunday = isSunday,
            thereIsProtectors = thereIsProtectors,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun DayShiftsAndEvents(
    dayShifts: DayShifts,
    events: List<GoogleCalendarEvent>,
    onDeleteVolunteer: (VolunteerId) -> Unit,
    isSunday: Boolean,
    thereIsProtectors: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (events.isNotEmpty()) {
            item {
                ShiftTitle(
                    text = stringResource(Res.string.special_activities),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            items(events) { event ->
                EventItem(event)
            }
        }
        item {
            ShiftTitle(
                text = stringResource(Res.string.all_day),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        items(dayShifts.allDayVolunteers) { volunteer ->
            VolunteerShiftItem(
                volunteer = volunteer,
                onClickDelete = { onDeleteVolunteer(volunteer.id) },
            )
        }
        item {
            if (isSunday) {
                ShiftTitle(text = stringResource(Res.string.shift_morning_sunday))
            } else {
                ShiftTitle(text = stringResource(Res.string.shift_morning))
            }
        }
        items(dayShifts.morningVolunteers) { volunteer ->
            VolunteerShiftItem(
                volunteer = volunteer,
                onClickDelete = { onDeleteVolunteer(volunteer.id) },
            )
        }
        item {
            when {
                isSunday && thereIsProtectors -> {
                    ShiftTitle(text = stringResource(Res.string.shift_afternoon_sunday_with_puja))
                }
                isSunday -> {
                    ShiftTitle(
                        text = stringResource(Res.string.shift_afternoon_sunday_without_puja),
                    )
                }
                else -> ShiftTitle(text = stringResource(Res.string.shift_afternoon))
            }
        }
        items(dayShifts.afternoonVolunteers) { volunteer ->
            VolunteerShiftItem(
                volunteer = volunteer,
                onClickDelete = { onDeleteVolunteer(volunteer.id) },
            )
        }
    }
}

@Composable
private fun EventItem(
    event: GoogleCalendarEvent,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
            )
            if (!event.isAllDay) {
                CVTag(
                    text = "${event.start.time.format("HH:mm")}-${event.end.time.format("HH:mm")}",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ShiftTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(top = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun VolunteerShiftItem(
    volunteer: VolunteerItemUi,
    modifier: Modifier = Modifier,
    onClickDelete: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
                ) {
                    Text(
                        text = volunteer.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = volunteer.schedule,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (volunteer.canBeDeleted) {
                    IconButton(onClick = onClickDelete) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_delete),
                            contentDescription = null,
                        )
                    }
                }
            }
            when (val volunteerType = volunteer.type) {
                is VolunteerTypeUi.Single -> {
                    CVTag(
                        text = volunteerType.type.displayName(),
                        icon = volunteerType.type.getIcon(),
                        backgroundColor = volunteerType.type.getBackgroundColor(),
                    )
                }
                is VolunteerTypeUi.AllDay -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CVTag(
                            text = stringResource(
                                Res.string.morning_shift_tag,
                                volunteerType.morning.displayName(),
                            ),
                            icon = painterResource(Res.drawable.ic_sun),
                            backgroundColor = volunteerType.morning.getBackgroundColor(),
                        )
                        CVTag(
                            text = stringResource(
                                Res.string.afternoon_shift_tag,
                                volunteerType.afternoon.displayName(),
                            ),
                            icon = painterResource(Res.drawable.ic_afternoon),
                            backgroundColor = volunteerType.afternoon.getBackgroundColor(),
                        )
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = BorderColor,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                volunteer.meals.forEach { meal ->
                    meal.getIcon()?.let { icon ->
                        CVTag(
                            text = meal.displayName(),
                            icon = icon,
                            backgroundColor = meal.getBackgroundColor(),
                        )
                    }
                }
                if (volunteer.sleep) {
                    CVTag(
                        text = stringResource(Res.string.overnight_stay),
                        icon = painterResource(Res.drawable.ic_sleep_bed),
                        backgroundColor = Color(0xFFD3AD63),
                    )
                }
            }
        }
    }
}

private val BorderColor = Color(0xFFE9E8E7)
