package com.casavirupa.voluntariat.features.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.core.utils.formatString
import com.casavirupa.voluntariat.shared.designsystem.components.CVTag
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.designsystem.components.WarningDialog
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.history.generated.resources.Res
import voluntariatcv.features.history.generated.resources.accept_button
import voluntariatcv.features.history.generated.resources.afternoon
import voluntariatcv.features.history.generated.resources.confirm_pay
import voluntariatcv.features.history.generated.resources.confirm_payment_description
import voluntariatcv.features.history.generated.resources.confirm_payment_title
import voluntariatcv.features.history.generated.resources.days
import voluntariatcv.features.history.generated.resources.general
import voluntariatcv.features.history.generated.resources.hours
import voluntariatcv.features.history.generated.resources.hours_format
import voluntariatcv.features.history.generated.resources.ic_afternoon
import voluntariatcv.features.history.generated.resources.ic_arrow_left
import voluntariatcv.features.history.generated.resources.ic_arrow_right
import voluntariatcv.features.history.generated.resources.ic_calendar_today
import voluntariatcv.features.history.generated.resources.ic_cancel
import voluntariatcv.features.history.generated.resources.ic_clock
import voluntariatcv.features.history.generated.resources.ic_group
import voluntariatcv.features.history.generated.resources.ic_sun
import voluntariatcv.features.history.generated.resources.ic_target
import voluntariatcv.features.history.generated.resources.morning
import voluntariatcv.features.history.generated.resources.my_volunteerings
import voluntariatcv.features.history.generated.resources.no_volunteering_description
import voluntariatcv.features.history.generated.resources.no_volunteering_title
import voluntariatcv.features.history.generated.resources.pending_payment
import voluntariatcv.features.history.generated.resources.remember_payment
import voluntariatcv.features.history.generated.resources.specific
import voluntariatcv.features.history.generated.resources.volunteerings_history

@Composable
internal fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel()) {
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showPaymentDialog by viewModel.showPaymentDialog.collectAsStateWithLifecycle()

    HistoryContent(
        currentDate = currentDate,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        uiState = uiState,
        onPayVolunteer = viewModel::showPaymentDialog,
    )

    if (showPaymentDialog) {
        WarningDialog(
            onDismiss = viewModel::closePaymentDialog,
            title = stringResource(Res.string.confirm_payment_title),
            description = stringResource(Res.string.confirm_payment_description),
            onCancel = viewModel::closePaymentDialog,
            confirmText = stringResource(Res.string.accept_button),
            onConfirm = viewModel::confirmPayment,
        )
    }
}

@Composable
private fun HistoryContent(
    currentDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    uiState: HistoryUiState,
    onPayVolunteer: () -> Unit,
) {
    Scaffold(
        topBar = {
            Header(
                currentDate = currentDate,
                onClickPreviousMonth = onPreviousMonth,
                onClickNextMonth = onNextMonth,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            InformationSummary(
                info = uiState.summary,
                modifier = Modifier.padding(top = 16.dp),
            )
            when (val state = uiState.paymentUiState) {
                PaymentUiState.NotFound -> PaymentMessage(
                    title = stringResource(Res.string.no_volunteering_title),
                    description = stringResource(Res.string.no_volunteering_description),
                )
                PaymentUiState.Paid -> {}
                is PaymentUiState.NotPaid -> PaymentMessage(
                    title = stringResource(Res.string.pending_payment),
                    description = stringResource(Res.string.remember_payment, state.amount),
                    onClickPay = onPayVolunteer,
                )
            }
            HistoryList(
                history = uiState.volunteers,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Header(
    currentDate: LocalDate,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MediumTopBar(title = stringResource(Res.string.my_volunteerings))
        MonthSelector(
            currentDate = currentDate,
            onClickPreviousMonth = onClickPreviousMonth,
            onClickNextMonth = onClickNextMonth,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MonthSelector(
    currentDate: LocalDate,
    onClickPreviousMonth: () -> Unit,
    onClickNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClickPreviousMonth) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left),
                contentDescription = null,
            )
        }
        Text(
            text = currentDate.format("MMMM yyyy").uppercase(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onClickNextMonth) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_right),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun InformationSummary(
    info: MonthSummary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryContainer(
            title = stringResource(Res.string.hours),
            icon = painterResource(Res.drawable.ic_clock),
            value = info.hours,
            modifier = Modifier.weight(1f),
        )
        SummaryContainer(
            title = stringResource(Res.string.days),
            icon = painterResource(Res.drawable.ic_calendar_today),
            value = info.days,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryContainer(
    title: String,
    icon: Painter,
    value: DetailedSummary,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                text = when (value.total) {
                    is Double -> value.total.formatString(1)
                    else -> value.total.toString()
                },
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            DetailedSummaryInfo(
                icon = painterResource(Res.drawable.ic_group),
                title = stringResource(Res.string.general),
                total = value.general,
                iconColor = Color(0xFF8FA399),
            )
            DetailedSummaryInfo(
                icon = painterResource(Res.drawable.ic_target),
                title = stringResource(Res.string.specific),
                total = value.specific,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DetailedSummaryInfo(
    icon: Painter,
    title: String,
    total: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Text(
            text = total,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun PaymentMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClickPay: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_cancel),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                )
            }
            if (onClickPay != null) {
                TextButton(onClick = onClickPay) {
                    Text(text = stringResource(Res.string.confirm_pay))
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: List<VolunteerHistoryItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.volunteerings_history).uppercase(),
            modifier = Modifier.padding(bottom = 12.dp),
            style = MaterialTheme.typography.labelLarge,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            history.forEach {
                HistoryItem(it)
            }
        }
    }
}

@Composable
private fun HistoryItem(
    volunteer: VolunteerHistoryItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volunteer.date.format("d MMMM"),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(Res.string.hours_format, volunteer.hours),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Column {
                ShiftTags(shifts = volunteer.shifts)
            }
        }
    }
}

@Composable
private fun ShiftTags(
    shifts: List<Shift>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        shifts.forEach { shift ->
            when (shift) {
                is Shift.Morning -> CVTag(
                    text = stringResource(Res.string.morning),
                    icon = painterResource(Res.drawable.ic_sun),
                    backgroundColor = Color(0xFFC2A47D),
                )
                is Shift.Afternoon -> CVTag(
                    text = stringResource(Res.string.afternoon),
                    icon = painterResource(Res.drawable.ic_afternoon),
                    backgroundColor = Color(0xFF9E816E),
                )
            }
        }
    }
}