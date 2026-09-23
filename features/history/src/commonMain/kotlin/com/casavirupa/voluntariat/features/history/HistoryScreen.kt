package com.casavirupa.voluntariat.features.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.core.utils.formatString
import com.casavirupa.voluntariat.shared.designsystem.components.CVButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVOutlinedButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTag
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.designsystem.components.WarningDialog
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.history.generated.resources.Res
import voluntariatcv.features.history.generated.resources.afternoon
import voluntariatcv.features.history.generated.resources.close_button
import voluntariatcv.features.history.generated.resources.delete
import voluntariatcv.features.history.generated.resources.delete_volunteering_description
import voluntariatcv.features.history.generated.resources.delete_volunteering_title
import voluntariatcv.features.history.generated.resources.dinners_count
import voluntariatcv.features.history.generated.resources.general
import voluntariatcv.features.history.generated.resources.hours
import voluntariatcv.features.history.generated.resources.hours_format
import voluntariatcv.features.history.generated.resources.ic_afternoon
import voluntariatcv.features.history.generated.resources.ic_arrow_left
import voluntariatcv.features.history.generated.resources.ic_arrow_right
import voluntariatcv.features.history.generated.resources.ic_cancel
import voluntariatcv.features.history.generated.resources.ic_clock
import voluntariatcv.features.history.generated.resources.ic_delete
import voluntariatcv.features.history.generated.resources.ic_group
import voluntariatcv.features.history.generated.resources.ic_sun
import voluntariatcv.features.history.generated.resources.ic_target
import voluntariatcv.features.history.generated.resources.breakfasts_count
import voluntariatcv.features.history.generated.resources.breakfasts_next_day_count
import voluntariatcv.features.history.generated.resources.lunches_count
import voluntariatcv.features.history.generated.resources.mitra_free_breakfasts_next_day_count
import voluntariatcv.features.history.generated.resources.mitra_free_meals_count
import voluntariatcv.features.history.generated.resources.mitra_free_nights_count
import voluntariatcv.features.history.generated.resources.morning
import voluntariatcv.features.history.generated.resources.my_volunteerings
import voluntariatcv.features.history.generated.resources.nights_count
import voluntariatcv.features.history.generated.resources.no_volunteering_description
import voluntariatcv.features.history.generated.resources.no_volunteering_title
import voluntariatcv.features.history.generated.resources.nothing_to_pay
import voluntariatcv.features.history.generated.resources.pay_button
import voluntariatcv.features.history.generated.resources.payment_balance
import voluntariatcv.features.history.generated.resources.payment_detail_balance
import voluntariatcv.features.history.generated.resources.payment_detail_button
import voluntariatcv.features.history.generated.resources.payment_detail_title
import voluntariatcv.features.history.generated.resources.payment_detail_total_month
import voluntariatcv.features.history.generated.resources.payment_month_amount
import voluntariatcv.features.history.generated.resources.pending_payment
import voluntariatcv.features.history.generated.resources.specific
import voluntariatcv.features.history.generated.resources.volunteerings_history

@Composable
internal fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel()) {
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showPaymentDetailDialog by viewModel
        .showPaymentDetailDialog.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()

    HistoryContent(
        currentDate = currentDate,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        uiState = uiState,
        onShowPaymentDetail = viewModel::showPaymentDetailDialog,
        onDeleteVolunteer = viewModel::onDeleteVolunteer,
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

    if (showPaymentDetailDialog) {
        PaymentDetailDialog(
            detail = uiState.paymentDetail,
            monthName = currentDate.monthName(),
            onDismiss = viewModel::closePaymentDetailDialog,
        )
    }
}

@Composable
private fun HistoryContent(
    currentDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    uiState: HistoryUiState,
    onShowPaymentDetail: () -> Unit,
    onDeleteVolunteer: (VolunteerId) -> Unit,
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
            if (uiState.showNoVolunteeringMessage) {
                PaymentMessage(
                    title = stringResource(Res.string.no_volunteering_title),
                    lines = listOf(stringResource(Res.string.no_volunteering_description)),
                )
            }
            when (val state = uiState.paymentUiState) {
                PaymentUiState.Hidden -> {}
                is PaymentUiState.Pending -> PaymentMessage(
                    title = stringResource(Res.string.pending_payment),
                    lines = listOf(
                        stringResource(
                            Res.string.payment_month_amount,
                            currentDate.monthName(),
                            state.monthAmount.toAmountString(),
                        ),
                        stringResource(
                            Res.string.payment_balance,
                            state.balance.toAmountString(),
                        ),
                    ),
                    onClickDetail = onShowPaymentDetail,
                )
            }
            HistoryList(
                history = uiState.volunteers,
                onDeleteVolunteer = onDeleteVolunteer,
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
    info: DetailedSummary,
    modifier: Modifier = Modifier,
) {
    SummaryContainer(
        title = stringResource(Res.string.hours),
        icon = painterResource(Res.drawable.ic_clock),
        value = info,
        modifier = modifier.fillMaxWidth(),
    )
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
                text = value.total.toString(),
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
                iconColor = MaterialTheme.colorScheme.secondary,
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
    lines: List<String>,
    modifier: Modifier = Modifier,
    onClickDetail: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                lines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
            }
            if (onClickDetail != null) {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        onClick = onClickDetail,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(text = stringResource(Res.string.payment_detail_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    history: List<VolunteerHistoryItem>,
    onDeleteVolunteer: (VolunteerId) -> Unit,
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
                HistoryItem(
                    volunteer = it,
                    onClickDelete = { onDeleteVolunteer(it.id) },
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    volunteer: VolunteerHistoryItem,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            if (volunteer.canBeDeleted) {
                IconButton(onClick = onClickDelete) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDetailDialog(
    detail: PaymentDetail,
    monthName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    ) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            Text(
                text = stringResource(Res.string.payment_detail_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(24.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (detail.breakfasts > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.breakfasts_count,
                            detail.breakfasts,
                            detail.breakfasts,
                        ),
                        amount = detail.breakfastsAmount,
                    )
                }
                if (detail.lunches > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.lunches_count,
                            detail.lunches,
                            detail.lunches,
                        ),
                        amount = detail.lunchesAmount,
                    )
                }
                if (detail.dinners > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.dinners_count,
                            detail.dinners,
                            detail.dinners,
                        ),
                        amount = detail.dinnersAmount,
                    )
                }
                // One pool for lunches, dinners and same-day breakfasts
                if (detail.freeMeals > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.mitra_free_meals_count,
                            detail.freeMeals,
                            detail.freeMeals,
                        ),
                        amount = -detail.mealsDiscount,
                    )
                }
                if (detail.nights > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.nights_count,
                            detail.nights,
                            detail.nights,
                        ),
                        amount = detail.nightsAmount,
                    )
                }
                if (detail.freeNights > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.mitra_free_nights_count,
                            detail.freeNights,
                            detail.freeNights,
                        ),
                        amount = -detail.nightsDiscount,
                    )
                }
                if (detail.breakfastsNextDay > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.breakfasts_next_day_count,
                            detail.breakfastsNextDay,
                            detail.breakfastsNextDay,
                        ),
                        amount = detail.breakfastsNextDayAmount,
                    )
                }
                if (detail.freeBreakfastsNextDay > 0) {
                    PaymentDetailRow(
                        concept = pluralStringResource(
                            Res.plurals.mitra_free_breakfasts_next_day_count,
                            detail.freeBreakfastsNextDay,
                            detail.freeBreakfastsNextDay,
                        ),
                        amount = -detail.breakfastsNextDayDiscount,
                    )
                }
                if (detail.total == 0.0) {
                    Text(
                        text = stringResource(Res.string.nothing_to_pay),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PaymentDetailRow(
                        concept = stringResource(Res.string.payment_detail_total_month, monthName),
                        amount = detail.total,
                        emphasized = true,
                    )
                }
                // The balance spans every month (minus what's already been paid): it is
                // what the volunteer actually owes and what the payment link carries.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PaymentDetailRow(
                    concept = stringResource(Res.string.payment_detail_balance),
                    amount = detail.balance,
                    emphasized = true,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CVOutlinedButton(
                        text = stringResource(Res.string.close_button),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    detail.paymentUrl?.let { url ->
                        CVButton(
                            text = stringResource(Res.string.pay_button),
                            onClick = { uriHandler.openUri(url) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(
    concept: String,
    amount: Double,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val style = if (emphasized) {
        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(modifier = modifier) {
        Text(
            text = concept,
            modifier = Modifier.weight(1f),
            style = style,
        )
        Text(
            text = "${amount.toAmountString()}€",
            style = style,
        )
    }
}

private fun Double.toAmountString(): String =
    if (this % 1.0 == 0.0) toInt().toString() else formatString(2)

private fun LocalDate.monthName(): String =
    format("MMMM").replaceFirstChar { it.uppercase() }

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