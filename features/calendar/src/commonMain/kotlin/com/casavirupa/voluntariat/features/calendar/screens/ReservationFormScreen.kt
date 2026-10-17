package com.casavirupa.voluntariat.features.calendar.screens

import com.casavirupa.voluntariat.shared.model.calendar.CasaVirupaTimeZone
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.features.calendar.viewmodels.AdditionalOption
import com.casavirupa.voluntariat.features.calendar.viewmodels.FormVolunteerTypeUi
import com.casavirupa.voluntariat.features.calendar.viewmodels.RemoteDialog
import com.casavirupa.voluntariat.features.calendar.viewmodels.ReservationFormError
import com.casavirupa.voluntariat.features.calendar.viewmodels.ReservationFormViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.ShiftEditor
import com.casavirupa.voluntariat.features.calendar.viewmodels.ShiftInfoSummary
import com.casavirupa.voluntariat.features.calendar.viewmodels.ShiftUi
import com.casavirupa.voluntariat.shared.common.ui.displayName
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTag
import com.casavirupa.voluntariat.shared.designsystem.components.DateTextField
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.designsystem.components.TimeTextField
import com.casavirupa.voluntariat.shared.designsystem.components.WarningDialog
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.accept
import voluntariatcv.features.calendar.generated.resources.additional_options
import voluntariatcv.features.calendar.generated.resources.afternoon_label
import voluntariatcv.features.calendar.generated.resources.afternoon_shift_title
import voluntariatcv.features.calendar.generated.resources.confirm
import voluntariatcv.features.calendar.generated.resources.date_placeholder
import voluntariatcv.features.calendar.generated.resources.end_label
import voluntariatcv.features.calendar.generated.resources.ic_calendar_today
import voluntariatcv.features.calendar.generated.resources.ic_clock
import voluntariatcv.features.calendar.generated.resources.ic_close
import voluntariatcv.features.calendar.generated.resources.ic_delete
import voluntariatcv.features.calendar.generated.resources.ic_edit
import voluntariatcv.features.calendar.generated.resources.ic_sleep_bed
import voluntariatcv.features.calendar.generated.resources.morning_label
import voluntariatcv.features.calendar.generated.resources.morning_shift_title
import voluntariatcv.features.calendar.generated.resources.not_available_no_online_areas_description
import voluntariatcv.features.calendar.generated.resources.not_available_volunteering_description
import voluntariatcv.features.calendar.generated.resources.not_available_volunteering_title
import voluntariatcv.features.calendar.generated.resources.online_shift_label
import voluntariatcv.features.calendar.generated.resources.online_tag
import voluntariatcv.features.calendar.generated.resources.sleep_notice_non_member
import voluntariatcv.features.calendar.generated.resources.reservation_error_missing_date
import voluntariatcv.features.calendar.generated.resources.reservation_error_missing_shift
import voluntariatcv.features.calendar.generated.resources.reservation_error_missing_shift_or_option
import voluntariatcv.features.calendar.generated.resources.reservation_error_missing_specific_area
import voluntariatcv.features.calendar.generated.resources.reservation_error_online_area_required
import voluntariatcv.features.calendar.generated.resources.reservation_error_overlapping_shifts
import voluntariatcv.features.calendar.generated.resources.reservation_error_overlaps_booked_shift
import voluntariatcv.features.calendar.generated.resources.reservation_error_save_failed
import voluntariatcv.features.calendar.generated.resources.reservation_error_title
import voluntariatcv.features.calendar.generated.resources.reservation_form_title
import voluntariatcv.features.calendar.generated.resources.select_date
import voluntariatcv.features.calendar.generated.resources.select_specific_area
import voluntariatcv.features.calendar.generated.resources.shift_label
import voluntariatcv.features.calendar.generated.resources.shift_optional_hint
import voluntariatcv.features.calendar.generated.resources.shift_overlap_error
import voluntariatcv.features.calendar.generated.resources.sunday_end_warning
import voluntariatcv.features.calendar.generated.resources.start_label
import kotlin.time.Clock

@Composable
internal fun ReservationFormScreen(
    onNavBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReservationFormViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val optionsSelected by viewModel.additionalOptionsSelected.collectAsStateWithLifecycle()
    val options by viewModel.additionalOptions.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val shiftsInfo by viewModel.shiftsInfo.collectAsStateWithLifecycle()
    val specificAreas by viewModel.specificAreas.collectAsStateWithLifecycle()
    val showSpecificAreaSelector by viewModel.showSpecificAreaSelector.collectAsStateWithLifecycle()
    val remoteDialog by viewModel.remoteDialog.collectAsStateWithLifecycle()
    val showSleepNotice by viewModel.showSleepNotice.collectAsStateWithLifecycle()
    val volunteerTypeOptions by viewModel.volunteerTypeOptions.collectAsStateWithLifecycle()
    val showOnlineToggle by viewModel.showOnlineToggle.collectAsStateWithLifecycle()
    val forcedOnline by viewModel.forcedOnline.collectAsStateWithLifecycle()
    val sundayEndWarning by viewModel.sundayEndWarning.collectAsStateWithLifecycle()
    val overlappingShift by viewModel.overlappingShift.collectAsStateWithLifecycle()
    val lateSundayShifts by viewModel.lateSundayShifts.collectAsStateWithLifecycle()

    ReservationFormContent(
        onNavBack = onNavBack,
        date = date,
        shiftsInfo = shiftsInfo,
        lateSundayShifts = lateSundayShifts,
        optionsSelected = optionsSelected,
        options = options,
        showSleepNotice = showSleepNotice,
        onDateChanged = viewModel::onDateChanged,
        onAddShift = viewModel::onAddShift,
        onAdditionalOptionSelected = viewModel::onAdditionOptionSelected,
        onEditShift = viewModel::onEditShift,
        onRemoveShift = viewModel::onRemoveShift,
        onConfirm = viewModel::onConfirm,
    )

    editor?.let { shift ->
        ShiftModal(
            editor = shift,
            typeOptions = volunteerTypeOptions,
            onVolunteerTypeChanged = viewModel::onVolunteerTypeChanged,
            showOnlineToggle = showOnlineToggle,
            onlineLocked = forcedOnline,
            onOnlineChanged = viewModel::onOnlineChanged,
            sundayEndWarning = sundayEndWarning,
            overlappingShift = overlappingShift,
            onDismiss = viewModel::dismissModal,
            onStartTimeChanged = viewModel::onStartTimeChanged,
            onEndTimeChanged = viewModel::onEndTimeChanged,
            onConfirm = viewModel::onConfirmShift,
            specificAreas = specificAreas,
            showSpecificAreasSelector = showSpecificAreaSelector,
            onSpecificAreaChanged = viewModel::onSpecificAreaChanged,
        )
    }
    
    uiState.error?.let { error ->
        WarningDialog(
            onDismiss = viewModel::dismissError,
            title = stringResource(Res.string.reservation_error_title),
            description = stringResource(error.message()),
            confirmText = stringResource(Res.string.accept),
            onConfirm = viewModel::dismissError,
        )
    }

    when (remoteDialog) {
        RemoteDialog.CanGoOnline -> WarningDialog(
            onDismiss = viewModel::closeRemoteDialog,
            title = stringResource(Res.string.not_available_volunteering_title),
            description = stringResource(Res.string.not_available_volunteering_description),
            onCancel = viewModel::closeRemoteDialog,
            confirmText = stringResource(Res.string.accept),
            onConfirm = viewModel::workOnRemoteOnDate,
        )
        RemoteDialog.NoOnlineAreas -> WarningDialog(
            onDismiss = viewModel::closeRemoteDialog,
            title = stringResource(Res.string.not_available_volunteering_title),
            description = stringResource(Res.string.not_available_no_online_areas_description),
            onCancel = viewModel::closeRemoteDialog,
            confirmText = stringResource(Res.string.accept),
            onConfirm = viewModel::closeRemoteDialog,
        )
        RemoteDialog.None -> {}
    }

    val currentOnSaved by rememberUpdatedState(onSaved)
    LaunchedEffect(uiState.isFormSavedSuccessfully) {
        if (uiState.isFormSavedSuccessfully) {
            currentOnSaved()
            viewModel.onNavigationHandled()
        }
    }
}

@Composable
private fun ReservationFormContent(
    onNavBack: () -> Unit,
    date: LocalDate?,
    shiftsInfo: List<ShiftInfoSummary>,
    lateSundayShifts: Map<Long, LocalTime>,
    optionsSelected: List<AdditionalOption>,
    options: List<AdditionalOption>,
    showSleepNotice: Boolean,
    onDateChanged: (LocalDate) -> Unit,
    onAddShift: (ShiftUi) -> Unit,
    onAdditionalOptionSelected: (AdditionalOption) -> Unit,
    onEditShift: (Long) -> Unit,
    onRemoveShift: (Long) -> Unit,
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
                shiftsInfo = shiftsInfo,
                lateSundayShifts = lateSundayShifts,
                optionsSelected = optionsSelected,
                options = options,
                showSleepNotice = showSleepNotice,
                onDateChanged = onDateChanged,
                onAddShift = onAddShift,
                onAdditionalOptionSelected = onAdditionalOptionSelected,
                onEditShift = onEditShift,
                onRemoveShift = onRemoveShift,
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
    shiftsInfo: List<ShiftInfoSummary>,
    lateSundayShifts: Map<Long, LocalTime>,
    optionsSelected: List<AdditionalOption>,
    options: List<AdditionalOption>,
    showSleepNotice: Boolean,
    onDateChanged: (LocalDate) -> Unit,
    onAddShift: (ShiftUi) -> Unit,
    onAdditionalOptionSelected: (AdditionalOption) -> Unit,
    onEditShift: (Long) -> Unit,
    onRemoveShift: (Long) -> Unit,
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
                    .toLocalDateTime(CasaVirupaTimeZone)
                    .date,
            )
        }
        FormSection(
            title = stringResource(Res.string.shift_label),
            icon = painterResource(Res.drawable.ic_calendar_today),
        ) {
            ShiftSelector(
                onAddShift = onAddShift,
                shiftsInfo = shiftsInfo,
                lateSundayShifts = lateSundayShifts,
                onEditInfo = onEditShift,
                onRemove = onRemoveShift,
            )
            // Meals and a bed can be booked on their own (issue #108)
            if (shiftsInfo.isEmpty() && options.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.shift_optional_hint),
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Long-stay volunteers have no meals or nights to book, and forced-online days have
        // none either, so the section is hidden
        if (options.isNotEmpty()) {
            FormSection(
                title = stringResource(Res.string.additional_options),
                icon = painterResource(Res.drawable.ic_calendar_today),
            ) {
                AdditionalOptionsSelector(
                    options = options,
                    optionsSelected = optionsSelected,
                    onSelectOption = onAdditionalOptionSelected,
                )
                if (showSleepNotice) {
                    SleepNotice(modifier = Modifier.padding(top = 12.dp))
                }
            }
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
    onAddShift: (ShiftUi) -> Unit,
    shiftsInfo: List<ShiftInfoSummary>,
    lateSundayShifts: Map<Long, LocalTime>,
    onEditInfo: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // A chip always adds a new shift (issue #113): once its half-day has one it's
            // highlighted and shows a «+» to say another can be added; each shift is removed
            // from its own row
            ShiftUi.entries.forEach { shift ->
                val hasShifts = shiftsInfo.any { it.shift == shift }
                FormChip(
                    text = if (hasShifts) "+ ${stringResource(shift.text)}" else stringResource(shift.text),
                    isSelected = hasShifts,
                    icon = painterResource(shift.icon),
                    onClick = { onAddShift(shift) },
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shiftsInfo.forEach { shift ->
                key(shift.id) {
                    ShiftScheduleInfo(
                        shift = shift,
                        sundayEndTime = lateSundayShifts[shift.id],
                        onClickEditInfo = { onEditInfo(shift.id) },
                        onClickRemove = { onRemove(shift.id) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalOptionsSelector(
    options: List<AdditionalOption>,
    optionsSelected: List<AdditionalOption>,
    onSelectOption: (AdditionalOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option in optionsSelected
            FormChip(
                text = stringResource(option.text),
                isSelected = isSelected,
                icon = painterResource(option.icon),
                onClick = { onSelectOption(option) },
            )
        }
    }
}

// Non-member habituals can't book a night in the app; they arrange it with the guesthouse.
// On-site volunteering ends earlier on Sundays (issue #105): only a warning, not a block.
@Composable
private fun SundayEndWarning(
    endTime: LocalTime,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_clock),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(Res.string.sunday_end_warning, endTime.format("HH:mm")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// The only thing that stops a shift from being added: overlapping another one (issue #113)
@Composable
private fun OverlapWarning(
    other: ShiftInfoSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_clock),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(Res.string.shift_overlap_error, other.displayTime()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SleepNotice(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_sleep_bed),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.sleep_notice_non_member),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
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
            if (icon != null) {
                Icon(
                    painter = icon,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp),
                    contentDescription = null,
                )
            }
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
    sundayEndTime: LocalTime?,
    onClickEditInfo: () -> Unit,
    onClickRemove: () -> Unit,
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
            val text = if (shift.type == FormVolunteerTypeUi.General) {
                stringResource(shift.type.text)
            } else {
                "${stringResource(shift.type.text)} · ${shift.specificArea?.displayName().orEmpty()}"
            }
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CVTag(
                    text = text,
                    icon = painterResource(shift.type.icon),
                    backgroundColor = shift.type.getBackgroundColor(),
                )
                if (shift.online) {
                    CVTag(
                        text = stringResource(Res.string.online_tag),
                        backgroundColor = OnlineTagColor,
                    )
                }
            }
            if (sundayEndTime != null) {
                SundayEndWarning(endTime = sundayEndTime, modifier = Modifier.padding(top = 8.dp))
            }
        }
        IconButton(onClick = onClickEditInfo) {
            Icon(
                painter = painterResource(Res.drawable.ic_edit),
                contentDescription = null,
            )
        }
        IconButton(onClick = onClickRemove) {
            Icon(
                painter = painterResource(Res.drawable.ic_delete),
                contentDescription = null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftModal(
    editor: ShiftEditor,
    typeOptions: List<FormVolunteerTypeUi>,
    onVolunteerTypeChanged: (FormVolunteerTypeUi) -> Unit,
    showOnlineToggle: Boolean,
    onlineLocked: Boolean,
    onOnlineChanged: (Boolean) -> Unit,
    sundayEndWarning: LocalTime?,
    overlappingShift: ShiftInfoSummary?,
    onDismiss: () -> Unit,
    onStartTimeChanged: (LocalTime) -> Unit,
    onEndTimeChanged: (LocalTime) -> Unit,
    onConfirm: () -> Unit,
    specificAreas: List<SpecificArea>,
    showSpecificAreasSelector: Boolean,
    onSpecificAreaChanged: (SpecificArea) -> Unit,
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
                text = stringResource(
                    when (editor.shift) {
                        ShiftUi.Morning -> Res.string.morning_shift_title
                        ShiftUi.Afternoon -> Res.string.afternoon_shift_title
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            FlowRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                typeOptions.forEach { volunteerType ->
                    FormChip(
                        text = stringResource(volunteerType.text),
                        isSelected = editor.type == volunteerType,
                        icon = painterResource(volunteerType.icon),
                        onClick = { onVolunteerTypeChanged(volunteerType) },
                    )
                }
            }
            AnimatedVisibility(visible = showSpecificAreasSelector) {
                Column {
                    Text(
                        text = stringResource(Res.string.select_specific_area).uppercase(),
                        modifier = Modifier.padding(bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    FlowRow(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        specificAreas.forEach { specificArea ->
                            FormChip(
                                text = specificArea.displayName(),
                                isSelected = specificArea == editor.specificArea,
                                onClick = { onSpecificAreaChanged(specificArea) },
                            )
                        }
                    }
                }
            }
            // Only for specific shifts in an area that allows remote work; locked on when
            // the day has no on-site volunteering
            AnimatedVisibility(visible = showOnlineToggle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.online_shift_label),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = editor.online,
                        onCheckedChange = onOnlineChanged,
                        enabled = !onlineLocked,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimeTextField(
                    time = editor.timeRange.start,
                    onTimeChanged = onStartTimeChanged,
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.start_label),
                    leadingIcon = painterResource(Res.drawable.ic_clock),
                )
                TimeTextField(
                    time = editor.timeRange.end,
                    onTimeChanged = onEndTimeChanged,
                    modifier = Modifier.weight(1f),
                    label = stringResource(Res.string.end_label),
                    leadingIcon = painterResource(Res.drawable.ic_clock),
                )
            }
            AnimatedVisibility(visible = overlappingShift != null) {
                overlappingShift?.let { OverlapWarning(other = it) }
            }
            AnimatedVisibility(visible = sundayEndWarning != null) {
                sundayEndWarning?.let { SundayEndWarning(endTime = it) }
            }
            CVButton(
                text = stringResource(Res.string.confirm),
                // Refused while the times overlap another shift; the warning above says why
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

private fun ReservationFormError.message(): StringResource =
    when (this) {
        ReservationFormError.MissingDate -> Res.string.reservation_error_missing_date
        ReservationFormError.MissingShift -> Res.string.reservation_error_missing_shift
        ReservationFormError.MissingShiftOrOption -> Res.string.reservation_error_missing_shift_or_option
        ReservationFormError.MissingSpecificArea -> Res.string.reservation_error_missing_specific_area
        ReservationFormError.OverlappingShifts -> Res.string.reservation_error_overlapping_shifts
        ReservationFormError.OverlapsBookedShift -> Res.string.reservation_error_overlaps_booked_shift
        ReservationFormError.OnlineAreaRequired -> Res.string.reservation_error_online_area_required
        ReservationFormError.SaveFailed -> Res.string.reservation_error_save_failed
    }

private val OnlineTagColor = Color(0xFF7BA7BC)

private const val DATE_PATTERN = "d MMMM yyyy"
