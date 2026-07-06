package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.features.calendar.viewmodels.AdditionalOption
import com.casavirupa.voluntariat.features.calendar.viewmodels.FormVolunteerTypeUi
import com.casavirupa.voluntariat.features.calendar.viewmodels.ReservationFormViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.ShiftInfoSummary
import com.casavirupa.voluntariat.features.calendar.viewmodels.ShiftUi
import com.casavirupa.voluntariat.features.calendar.viewmodels.ShownModal
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTag
import com.casavirupa.voluntariat.shared.designsystem.components.DateTextField
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.designsystem.components.TimeTextField
import com.casavirupa.voluntariat.shared.designsystem.components.WarningDialog
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.accept
import voluntariatcv.features.calendar.generated.resources.additional_options
import voluntariatcv.features.calendar.generated.resources.afternoon_label
import voluntariatcv.features.calendar.generated.resources.afternoon_shift_title
import voluntariatcv.features.calendar.generated.resources.confirm
import voluntariatcv.features.calendar.generated.resources.date_placeholder
import voluntariatcv.features.calendar.generated.resources.end_label
import voluntariatcv.features.calendar.generated.resources.existing_volunteer_dialog_description
import voluntariatcv.features.calendar.generated.resources.existing_volunteer_dialog_title
import voluntariatcv.features.calendar.generated.resources.ic_calendar_today
import voluntariatcv.features.calendar.generated.resources.ic_clock
import voluntariatcv.features.calendar.generated.resources.ic_close
import voluntariatcv.features.calendar.generated.resources.ic_edit
import voluntariatcv.features.calendar.generated.resources.morning_label
import voluntariatcv.features.calendar.generated.resources.morning_shift_title
import voluntariatcv.features.calendar.generated.resources.reservation_form_title
import voluntariatcv.features.calendar.generated.resources.select_date
import voluntariatcv.features.calendar.generated.resources.shift_label
import voluntariatcv.features.calendar.generated.resources.start_label
import kotlin.time.Clock

@Composable
internal fun ReservationFormScreen(
    onNavBack: () -> Unit,
    viewModel: ReservationFormViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val options by viewModel.additionalOptions.collectAsStateWithLifecycle()
    val shownModal by viewModel.shownModal.collectAsStateWithLifecycle()
    val morningVolunteerType by viewModel.morningVolunteerType.collectAsStateWithLifecycle()
    val afternoonVolunteerType by viewModel.afternoonVolunteerType.collectAsStateWithLifecycle()
    val morningTimeRange by viewModel.morningTimeRange.collectAsStateWithLifecycle()
    val afternoonTimeRange by viewModel.afternoonTimeRange.collectAsStateWithLifecycle()
    val shiftsInfo by viewModel.shiftsInfo.collectAsStateWithLifecycle()
    val showExistingVolunteerDialog by viewModel
        .showExistingVolunteerDialogError.collectAsStateWithLifecycle()
    val notAvailableDays by viewModel.notAvailableDays.collectAsStateWithLifecycle()

    ReservationFormContent(
        onNavBack = onNavBack,
        date = date,
        shifts = shifts,
        shiftsInfo = shiftsInfo,
        options = options,
        onDateChanged = viewModel::onDateChanged,
        onShiftSelected = viewModel::onShiftSelected,
        onAdditionalOptionSelected = viewModel::onAdditionOptionSelected,
        onEditShiftInfo = { shift ->
            when (shift) {
                ShiftUi.Morning -> viewModel.openMorningModal()
                ShiftUi.Afternoon -> viewModel.openAfternoonModal()
            }
        },
        onConfirm = viewModel::onConfirm,
        notAvailableDays = notAvailableDays,
    )

    when (shownModal) {
        ShownModal.MorningShift -> ShiftModal(
            title = stringResource(Res.string.morning_shift_title),
            type = morningVolunteerType,
            onVolunteerTypeChanged = viewModel::onMorningVolunteerTypeChanged,
            timeRange = morningTimeRange,
            onDismiss = viewModel::dismissModal,
            onStartTimeChanged = viewModel::onMorningStartTimeChanged,
            onEndTimeChanged = viewModel::onMorningEndTimeChanged,
            onConfirm = viewModel::onConfirmShift,
        )
        ShownModal.AfternoonShift -> ShiftModal(
            title = stringResource(Res.string.afternoon_shift_title),
            type = afternoonVolunteerType,
            onVolunteerTypeChanged = viewModel::onAfternoonVolunteerTypeChanged,
            timeRange = afternoonTimeRange,
            onDismiss = viewModel::dismissModal,
            onStartTimeChanged = viewModel::onAfternoonStartTimeChanged,
            onEndTimeChanged = viewModel::onAfternoonEndTimeChanged,
            onConfirm = viewModel::onConfirmShift,
        )
        else -> {}
    }
    
    if (showExistingVolunteerDialog) {
        WarningDialog(
            onDismiss = viewModel::closeExistVolunteerDialog,
            title = stringResource(Res.string.existing_volunteer_dialog_title),
            description = stringResource(Res.string.existing_volunteer_dialog_description),
            onCancel = viewModel::closeExistVolunteerDialog,
            confirmText = stringResource(Res.string.accept),
            onConfirm = viewModel::closeExistVolunteerDialog,
        )
    }

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
    shifts: List<ShiftUi>,
    shiftsInfo: List<ShiftInfoSummary>,
    options: List<AdditionalOption>,
    onDateChanged: (LocalDate) -> Unit,
    onShiftSelected: (ShiftUi) -> Unit,
    onAdditionalOptionSelected: (AdditionalOption) -> Unit,
    onEditShiftInfo: (ShiftUi) -> Unit,
    onConfirm: () -> Unit,
    notAvailableDays: List<LocalDate>,
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
                shifts = shifts,
                shiftsInfo = shiftsInfo,
                options = options,
                onDateChanged = onDateChanged,
                onShiftSelected = onShiftSelected,
                onAdditionalOptionSelected = onAdditionalOptionSelected,
                onEditShiftInfo = onEditShiftInfo,
                notAvailableDays = notAvailableDays,
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
    shifts: List<ShiftUi>,
    shiftsInfo: List<ShiftInfoSummary>,
    options: List<AdditionalOption>,
    onDateChanged: (LocalDate) -> Unit,
    onShiftSelected: (ShiftUi) -> Unit,
    onAdditionalOptionSelected: (AdditionalOption) -> Unit,
    onEditShiftInfo: (ShiftUi) -> Unit,
    notAvailableDays: List<LocalDate>,
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
                notAvailableDays = notAvailableDays,
            )
        }
        FormSection(
            title = stringResource(Res.string.shift_label),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            ShiftSelector(
                selectedShifts = shifts,
                onSelectShift = onShiftSelected,
                shiftsInfo = shiftsInfo,
                onEditInfo = onEditShiftInfo,
            )
        }
        FormSection(
            title = stringResource(Res.string.additional_options),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            AdditionalOptionsSelector(
                options = options,
                onSelectOption = onAdditionalOptionSelected,
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
    Column(modifier = modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
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
private fun ShiftSelector(
    selectedShifts: List<ShiftUi>,
    onSelectShift: (ShiftUi) -> Unit,
    shiftsInfo: List<ShiftInfoSummary>,
    onEditInfo: (ShiftUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShiftUi.entries.forEach { shift ->
                val isSelected = shift in selectedShifts
                FormChip(
                    text = stringResource(shift.text),
                    isSelected = isSelected,
                    icon = painterResource(shift.icon),
                    onClick = { onSelectShift(shift) },
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shiftsInfo.forEach { shift ->
                ShiftScheduleInfo(
                    shift = shift,
                    onClickEditInfo = { onEditInfo(shift.shift) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AdditionalOptionsSelector(
    options: List<AdditionalOption>,
    onSelectOption: (AdditionalOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AdditionalOption.entries.forEach { option ->
            val isSelected = option in options
            FormChip(
                text = stringResource(option.text),
                isSelected = isSelected,
                icon = painterResource(option.icon),
                onClick = { onSelectOption(option) },
            )
        }
    }
}

@Composable
private fun FormChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp),
                contentDescription = null,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ShiftScheduleInfo(
    shift: ShiftInfoSummary,
    onClickEditInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shift.displayTime(),
                style = MaterialTheme.typography.labelSmall,
            )
            CVTag(
                text = stringResource(shift.type.text),
                icon = painterResource(shift.type.icon),
                modifier = Modifier.padding(top = 8.dp),
                backgroundColor = shift.type.getBackgroundColor(),
            )
        }
        IconButton(onClick = onClickEditInfo) {
            Icon(
                painter = painterResource(Res.drawable.ic_edit),
                contentDescription = null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftModal(
    title: String,
    type: FormVolunteerTypeUi,
    onVolunteerTypeChanged: (FormVolunteerTypeUi) -> Unit,
    timeRange: TimeRange,
    onDismiss: () -> Unit,
    onStartTimeChanged: (LocalTime) -> Unit,
    onEndTimeChanged: (LocalTime) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            FlowRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormVolunteerTypeUi.entries.forEach { volunteerType ->
                    FormChip(
                        text = stringResource(volunteerType.text),
                        isSelected = type == volunteerType,
                        icon = painterResource(volunteerType.icon),
                        onClick = { onVolunteerTypeChanged(volunteerType) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimeTextField(
                    time = timeRange.start,
                    onTimeChanged = onStartTimeChanged,
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.start_label),
                    leadingIcon = painterResource(Res.drawable.ic_clock),
                )
                TimeTextField(
                    time = timeRange.end,
                    onTimeChanged = onEndTimeChanged,
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.end_label),
                    leadingIcon = painterResource(Res.drawable.ic_clock),
                )
            }
            CVButton(
                text = stringResource(Res.string.confirm),
                onClick = onConfirm,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShiftInfoSummary.displayTime(): String {
    val timeRangeStr = "${timeRange.start.format("HH:mm")}h-${timeRange.end.format("HH:mm")}"
    return when (shift) {
        ShiftUi.Morning -> stringResource(Res.string.morning_label, timeRangeStr)
        ShiftUi.Afternoon -> stringResource(Res.string.afternoon_label, timeRangeStr)
    }
}

@Composable
private fun FormVolunteerTypeUi.getBackgroundColor() =
    when (this) {
        FormVolunteerTypeUi.General -> Color(0xFFC2A47D)
        FormVolunteerTypeUi.Specific -> Color(0xFF9E816E)
    }


private const val DATE_PATTERN = "d MMMM yyyy"
