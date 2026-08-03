package com.casavirupa.voluntariat.features.profile

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.casavirupa.voluntariat.shared.common.ui.displayName
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.core.utils.formatString
import com.casavirupa.voluntariat.shared.designsystem.components.CVOutlinedButton
import com.casavirupa.voluntariat.shared.designsystem.components.CVTag
import com.casavirupa.voluntariat.shared.designsystem.components.MediumTopBar
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentTarget
import com.casavirupa.voluntariat.shared.model.configuration.InterestLinks
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserRole
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import voluntariatcv.features.profile.generated.resources.Res
import voluntariatcv.features.profile.generated.resources.casa_virupa
import voluntariatcv.features.profile.generated.resources.commitment_degree
import voluntariatcv.features.profile.generated.resources.email
import voluntariatcv.features.profile.generated.resources.first_quarter
import voluntariatcv.features.profile.generated.resources.fourth_quarter
import voluntariatcv.features.profile.generated.resources.full_name
import voluntariatcv.features.profile.generated.resources.general_volunteering
import voluntariatcv.features.profile.generated.resources.habitual
import voluntariatcv.features.profile.generated.resources.ic_arrow_left
import voluntariatcv.features.profile.generated.resources.ic_arrow_right
import voluntariatcv.features.profile.generated.resources.ic_check
import voluntariatcv.features.profile.generated.resources.ic_clock
import voluntariatcv.features.profile.generated.resources.ic_layers
import voluntariatcv.features.profile.generated.resources.ic_link
import voluntariatcv.features.profile.generated.resources.ic_mail
import voluntariatcv.features.profile.generated.resources.ic_person
import voluntariatcv.features.profile.generated.resources.ic_star
import voluntariatcv.features.profile.generated.resources.ic_target
import voluntariatcv.features.profile.generated.resources.interest_links
import voluntariatcv.features.profile.generated.resources.last_updated
import voluntariatcv.features.profile.generated.resources.log_out
import voluntariatcv.features.profile.generated.resources.member_of_casa_virupa
import voluntariatcv.features.profile.generated.resources.missing_hours
import voluntariatcv.features.profile.generated.resources.mitra
import voluntariatcv.features.profile.generated.resources.monthly_hours
import voluntariatcv.features.profile.generated.resources.no_hours_registered
import voluntariatcv.features.profile.generated.resources.profile_title
import voluntariatcv.features.profile.generated.resources.quarterly_hours
import voluntariatcv.features.profile.generated.resources.second_quarter
import voluntariatcv.features.profile.generated.resources.specific_areas
import voluntariatcv.features.profile.generated.resources.third_quarter
import voluntariatcv.features.profile.generated.resources.volunteer_type

@Composable
internal fun ProfileScreen(
    onNavigateToSignIn: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val quarter by viewModel.quarter.collectAsStateWithLifecycle()
    val hoursDone by viewModel.hoursDone.collectAsStateWithLifecycle()
    val commitmentTargets by viewModel.commitmentTargets.collectAsStateWithLifecycle()
    val interestLinks by viewModel.interestLinks.collectAsStateWithLifecycle()

    user?.let {
        ProfileContent(
            user = it,
            onLogOut = viewModel::logOut,
            currentMonth = currentMonth,
            quarter = quarter,
            hoursDone = hoursDone,
            commitmentTargets = commitmentTargets,
            interestLinks = interestLinks,
            onNextMonth = viewModel::nextMonth,
            onPreviousMonth = viewModel::previousMonth,
            onNextQuarter = viewModel::nextQuarter,
            onPreviousQuarter = viewModel::previousQuarter,
        )
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnNavigateToSignIn by rememberUpdatedState(onNavigateToSignIn)

    LaunchedEffect(viewModel, lifecycle) {
        viewModel.uiState
            .flowWithLifecycle(lifecycle)
            .collect { state ->
                if (state.navigateToSignIn) {
                    currentOnNavigateToSignIn()
                }
            }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    currentMonth: LocalDate,
    quarter: Quarter,
    hoursDone: HoursBreakdown,
    commitmentTargets: CommitmentTargets,
    interestLinks: InterestLinks,
    onNextMonth: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextQuarter: () -> Unit,
    onPreviousQuarter: () -> Unit,
    onLogOut: () -> Unit,
) {
    Scaffold(
        topBar = { Header(name = user.name) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserBasicInfo(
                name = user.name,
                email = user.email,
                modifier = Modifier.padding(top = 16.dp)
            )
            DegreeOfCompliance(
                hoursBreakdown = hoursDone,
                targets = commitmentTargets,
                currentMonth = currentMonth,
                quarter = quarter,
                isMitra = user.isMitra,
                onNextMonth = onNextMonth,
                onPreviousMonth = onPreviousMonth,
                onNextQuarter = onNextQuarter,
                onPreviousQuarter = onPreviousQuarter,
            )
            if (interestLinks.text.isNotBlank()) {
                InterestLinksSection(interestLinks = interestLinks)
            }
            if (user.role is UserRole.Volunteer) {
                VolunteerType(volunteerRole = user.role as UserRole.Volunteer)
                SpecificAreas(specificAreas = user.specificAreas)
            }
            if (user.isMember) {
                MemberCV()
            }
            CVOutlinedButton(
                text = stringResource(Res.string.log_out),
                onClick = onLogOut,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Header(
    name: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MediumTopBar(
            title = stringResource(Res.string.profile_title),
            subtitle = name.uppercase()
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun UserBasicInfo(
    name: String,
    email: String,
    modifier: Modifier = Modifier,
) {
    ContentSurface(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            UserItemInfo(
                icon = painterResource(Res.drawable.ic_person),
                title = stringResource(Res.string.full_name),
                value = name,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            UserItemInfo(
                icon = painterResource(Res.drawable.ic_mail),
                title = stringResource(Res.string.email),
                value = email,
            )
        }
    }
}

@Composable
private fun UserItemInfo(
    icon: Painter,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionIcon(
            icon = icon,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun DegreeOfCompliance(
    isMitra: Boolean,
    hoursBreakdown: HoursBreakdown,
    targets: CommitmentTargets,
    currentMonth: LocalDate,
    quarter: Quarter,
    onNextMonth: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextQuarter: () -> Unit,
    onPreviousQuarter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = hoursBreakdown.total.toDouble()
    val targetHours = targets.total?.targetHours?.takeIf { it > 0 }
    val percentage = targetHours?.let {
        ((done / it) * 100).coerceAtMost(100.0).formatString(1)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.commitment_degree).uppercase(),
            style = MaterialTheme.typography.labelMedium,
        )
        ContentSurface {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isMitra) {
                    TimeSelector(
                        currentTimeTitle = quarter.getTitle(),
                        onClickNext = onNextQuarter,
                        onClickPrevious = onPreviousQuarter,
                    )
                } else {
                    TimeSelector(
                        currentTimeTitle = currentMonth.format("MMMM yyyy"),
                        onClickNext = onNextMonth,
                        onClickPrevious = onPreviousMonth,
                    )
                }
                val titleText = if (isMitra) {
                    stringResource(Res.string.quarterly_hours)
                } else {
                    stringResource(Res.string.monthly_hours)
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                ).toSpanStyle()
                            ) {
                                append(done.formatString(1))
                            }
                            if (targetHours != null) {
                                append(" / ")
                                withStyle(
                                    style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                                ) {
                                    append("${targetHours.formatHours()}h")
                                }
                            } else {
                                withStyle(
                                    style = MaterialTheme.typography.bodyLarge.toSpanStyle()
                                ) {
                                    append("h")
                                }
                            }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    if (percentage != null) {
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (targetHours != null) {
                    LinearProgressIndicator(
                        progress = { (done / targetHours).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        drawStopIndicator = {},
                    )
                }
                val messageText = when {
                    done == 0.0 -> stringResource(Res.string.no_hours_registered)
                    targetHours != null && done < targetHours -> stringResource(
                        Res.string.missing_hours,
                        (targetHours - done).formatString(1)
                    )
                    else -> null
                }
                if (messageText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = messageText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                val hasAreaTargets = targets.general != null || targets.byArea.isNotEmpty()
                if (hoursBreakdown.total > 0 || hasAreaTargets) {
                    HoursBreakdownDetail(
                        hoursBreakdown = hoursBreakdown,
                        targets = targets,
                    )
                }
            }
        }
    }
}

@Composable
private fun HoursBreakdownDetail(
    hoursBreakdown: HoursBreakdown,
    targets: CommitmentTargets,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        HoursBreakdownRow(
            icon = painterResource(Res.drawable.ic_layers),
            title = stringResource(Res.string.general_volunteering),
            hours = hoursBreakdown.general,
            target = targets.general,
        )
        // Areas with hours done or a commitment target, so a target is visible
        // even before the first hour is registered in its area
        val areas = hoursBreakdown.specificByArea.keys + targets.byArea.keys
        areas.forEach { area ->
            HoursBreakdownRow(
                icon = painterResource(Res.drawable.ic_target),
                title = area.displayName(),
                hours = hoursBreakdown.specificByArea[area] ?: 0,
                target = targets.byArea[area],
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HoursBreakdownRow(
    icon: Painter,
    title: String,
    hours: Int,
    target: CommitmentTarget?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = if (target != null) {
                "${hours}h / ${target.targetHours.formatHours()}h"
            } else {
                "${hours}h"
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun TimeSelector(
    currentTimeTitle: String,
    onClickNext: () -> Unit,
    onClickPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClickPrevious) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left),
                contentDescription = null,
            )
        }
        Text(
            text = currentTimeTitle.uppercase(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onClickNext) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_right),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun InterestLinksSection(
    interestLinks: InterestLinks,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.interest_links).uppercase(),
            style = MaterialTheme.typography.labelMedium,
        )
        ContentSurface {
            Column(modifier = Modifier.animateContentSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionIcon(
                        icon = painterResource(Res.drawable.ic_link),
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.interest_links),
                            style = MaterialTheme.typography.bodyMedium
                                .copy(fontWeight = FontWeight.Medium),
                        )
                        interestLinks.updatedAt?.let { updatedAt ->
                            Text(
                                text = stringResource(
                                    Res.string.last_updated,
                                    updatedAt.format("dd/MM/yyyy"),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_right),
                        contentDescription = null,
                        modifier = Modifier.rotate(if (expanded) 90f else 0f),
                    )
                }
                if (expanded) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    SelectionContainer {
                        Text(
                            text = linkifiedText(interestLinks.text),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private val URL_REGEX = Regex("""(https?://|www\.)\S+""")

@Composable
private fun linkifiedText(text: String) = buildAnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    var lastIndex = 0
    URL_REGEX.findAll(text).forEach { match ->
        append(text.substring(lastIndex, match.range.first))
        val visibleUrl = match.value.trimEnd('.', ',', ';', ':', ')')
        val url = if (visibleUrl.startsWith("www.")) "https://$visibleUrl" else visibleUrl
        withLink(LinkAnnotation.Url(url = url, styles = linkStyles)) {
            append(visibleUrl)
        }
        lastIndex = match.range.first + visibleUrl.length
    }
    append(text.substring(lastIndex))
}

@Composable
private fun VolunteerType(
    volunteerRole: UserRole.Volunteer,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.volunteer_type).uppercase(),
            style = MaterialTheme.typography.labelMedium,
        )
        ContentSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionIcon(
                    icon = painterResource(Res.drawable.ic_star),
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = volunteerRole.getVolunteerType(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    /*
                    Text(
                        text = stringResource(Res.string.volunteer_dedication_description),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    */
                }
            }
        }
    }
}


@Composable
private fun SpecificAreas(
    specificAreas: List<SpecificArea>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.specific_areas).uppercase(),
            style = MaterialTheme.typography.labelMedium,
        )
        ContentSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionIcon(
                    icon = painterResource(Res.drawable.ic_layers),
                    modifier = Modifier.padding(end = 16.dp),
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specificAreas.forEach { area ->
                        CVTag(
                            text = area.displayName(),
                            icon = painterResource(Res.drawable.ic_target),
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCV(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.casa_virupa).uppercase(),
            style = MaterialTheme.typography.labelMedium,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF358932)),
            contentColor = Color(0xFF358932).copy(alpha = 0.2f)
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = Color(0xFF358932),
                )
                Text(
                    text = stringResource(Res.string.member_of_casa_virupa),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF358932),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionIcon(
    icon: Painter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
        ).padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun Double.formatHours(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        formatString(1)
    }

@Composable
private fun UserRole.Volunteer.getVolunteerType() =
    when (type) {
        UserVolunteerType.Habitual -> stringResource(Res.string.habitual)
        else -> stringResource(Res.string.mitra)
    }

@Composable
private fun Quarter.getTitle() =
    when (number) {
        1 -> stringResource(Res.string.first_quarter, year)
        2 -> stringResource(Res.string.second_quarter, year)
        3 -> stringResource(Res.string.third_quarter, year)
        else -> stringResource(Res.string.fourth_quarter, year)
    }
