package com.casavirupa.voluntariat.features.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.casavirupa.voluntariat.shared.designsystem.components.CVFabButton
import org.jetbrains.compose.resources.painterResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.ic_add
import voluntariatcv.features.calendar.generated.resources.ic_arrow_left
import voluntariatcv.features.calendar.generated.resources.ic_arrow_right

@Composable
internal fun CalendarScreen() {
    CalendarContent()
}

@Composable
private fun CalendarContent() {
    Scaffold(
        topBar = {
            CalendarTopBar(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .padding(start = 24.dp, end = 8.dp)
            )
        },
        floatingActionButton = {
            CVFabButton(
                icon = painterResource(Res.drawable.ic_add),
                onClick = {},
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {

    }
}

@Composable
private fun CalendarTopBar(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarTitles(modifier = Modifier.weight(2f))
        CalendarNavigationArrows(
            onClickPrevious = {},
            onClickNext = {},
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun TopBarTitles(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Octubre",
            modifier = Modifier.padding(bottom = 4.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = "Selecciona el dia de voluntariat".uppercase(),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CalendarNavigationArrows(
    onClickPrevious: () -> Unit,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onClickPrevious) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
        IconButton(onClick = onClickNext) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}