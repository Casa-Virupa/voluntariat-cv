package com.casavirupa.voluntariat.features.calendar.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casavirupa.voluntariat.features.calendar.components.MediumTopBar
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailUiState
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayShifts
import com.casavirupa.voluntariat.features.calendar.viewmodels.DetailHeaderUi
import com.casavirupa.voluntariat.features.calendar.viewmodels.VolunteerItemUi
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.ic_close
import voluntariatcv.features.calendar.generated.resources.num_of_volunteers

@Composable
fun DayDetailScreen(
    onNavBack: () -> Unit,
    viewModel: DayDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DayDetailContent(
        uiState = uiState,
        onClickBack = onNavBack,
        modifier = modifier,
    )
}

@Composable
private fun DayDetailContent(
    uiState: DayDetailUiState,
    onClickBack: () -> Unit,
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
                subtitle = stringResource(
                    Res.string.num_of_volunteers,
                    uiState.headerUi.numOfVolunteers,
                ).uppercase(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        DayShifts(
            dayShifts = uiState.dayShifts,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun DayShifts(
    dayShifts: DayShifts,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            ShiftTitle(text = "Tot el dia")
        }
        items(dayShifts.allDayVolunteers) { volunteer ->
            VolunteerShiftItem(volunteer)
        }
        item {
            ShiftTitle(text = "Torn de matí (10:00 - 14:00)")
        }
        items(dayShifts.morningVolunteers) { volunteer ->
            VolunteerShiftItem(volunteer)
        }
        item {
            ShiftTitle(text = "Torn de tarda (16:30 - 20:30)")
        }
        items(dayShifts.afternoonVolunteers) { volunteer ->
            VolunteerShiftItem(volunteer)
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
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun VolunteerShiftItem(
    volunteer: VolunteerItemUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = volunteer.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = volunteer.type.toString(),
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow {
                volunteer.meals.forEach { meal ->
                    Text(
                        text = meal.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (volunteer.sleep) {
                    Text(
                        text = "Pernoctació",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
