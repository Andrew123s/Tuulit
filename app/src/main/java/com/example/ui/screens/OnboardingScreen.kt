package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.TuulitRepository
import com.example.domain.BananaModePersona
import com.example.domain.CycleRegularity
import com.example.ui.components.BananaMascot
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitNoticeBanner
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.components.TuulitSecondaryButton
import com.example.ui.theme.TuulitThemeTokens
import kotlin.math.roundToInt

private data class OnboardingIntroStep(
    val badge: String,
    val title: String,
    val body: String,
    val persona: BananaModePersona,
    val icon: ImageVector,
)

private val introSteps = listOf(
    OnboardingIntroStep(
        badge = "WELCOME TO TUULIT",
        title = "A warmer companion for every day of your cycle.",
        body = "Track periods, understand patterns, find comfort on tough days, and keep a little humor close—without clinical coldness or guilt.",
        persona = BananaModePersona.NEUTRAL,
        icon = Icons.Outlined.FavoriteBorder
    ),
    OnboardingIntroStep(
        badge = "UNDERSTAND YOUR CYCLE",
        title = "Adaptive tracking built for real, variable bodies.",
        body = "Log flow, symptoms, moods, energy, and private notes. Tuulit never assumes a textbook 28-day schedule and clearly marks predictions as estimates.",
        persona = BananaModePersona.NEUTRAL,
        icon = Icons.Outlined.CalendarMonth
    ),
    OnboardingIntroStep(
        badge = "CARE & COMFORT",
        title = "Practical support when discomfort hits.",
        body = "Use Cramp Relief, guided breathing timers, Sleep Support, Mood Reset, and your own customizable Period Survival Kit.",
        persona = BananaModePersona.CRAMPS,
        icon = Icons.Outlined.Spa
    ),
    OnboardingIntroStep(
        badge = "PLAY & EXPLORE",
        title = "Genuinely fun distractions when you need a break.",
        body = "Play Banana Catch, Memory Flip, Calm Tap, or Stress Smash, and try informative quizzes with gentle, zero-pressure milestones.",
        persona = BananaModePersona.DRAMA,
        icon = Icons.Outlined.Games
    ),
    OnboardingIntroStep(
        badge = "YOUR PERSONAL COMPANION",
        title = "Clear, non-judgmental health answers.",
        body = "Explore Explain My Body, check common menstrual myths, and organize questions for medical visits—with strict safety guardrails and opt-in context.",
        persona = BananaModePersona.CHILL,
        icon = Icons.Outlined.AutoAwesome
    ),
    OnboardingIntroStep(
        badge = "STAY CONNECTED (OPTIONAL)",
        title = "Let trusted people support you on your terms.",
        body = "Optionally configure Tuulit Connect and 1-tap Tuulit SOS cards so partners, friends, or family know how to help—only with your permission.",
        persona = BananaModePersona.HAPPY,
        icon = Icons.Outlined.Group
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (
        displayName: String,
        lastPeriodStartEpochDay: Long?,
        cycleLength: Int,
        periodDuration: Int,
        regularity: CycleRegularity,
        notificationsEnabled: Boolean,
        aiContextOptIn: Boolean,
    ) -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) } // 0..5 = intro, 6 = personalization
    val totalSteps = 7

    var displayName by remember { mutableStateOf("") }
    var daysAgoOption by remember { mutableIntStateOf(-1) } // -1 = skip / unknown
    var cycleLength by remember { mutableIntStateOf(28) }
    var periodDuration by remember { mutableIntStateOf(5) }
    var regularity by remember { mutableStateOf(CycleRegularity.NOT_SURE) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var aiContextOptIn by remember { mutableStateOf(false) }

    val extended = TuulitThemeTokens.extended

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with Back + Step Indicators + Skip to Setup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stepIndex > 0) {
                    IconButton(
                        onClick = { stepIndex-- },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous onboarding step"
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { idx ->
                        val active = idx == stepIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (active) 22.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else extended.toastedSand
                                )
                        )
                    }
                }

                if (stepIndex < 6) {
                    TextButton(
                        onClick = { stepIndex = 6 },
                        modifier = Modifier.testTag("onboarding_skip_to_setup")
                    ) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Middle content
            if (stepIndex < introSteps.size) {
                val step = introSteps[stepIndex]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BananaMascot(persona = step.persona, size = 128.dp)
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(extended.warmTan)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(step.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = step.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = step.body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TuulitPrimaryButton(
                    text = if (stepIndex == introSteps.lastIndex) "Personalize Tuulit" else "Continue",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = { stepIndex++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_next_button")
                )
            } else {
                // Step 7 — Personalization
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BananaMascot(persona = BananaModePersona.NEUTRAL, size = 56.dp)
                        Column {
                            Text("Make Tuulit yours", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Skip anything you are unsure about—you can edit anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TuulitNoticeBanner(
                        title = "Predictions are always estimates",
                        message = "Bodies shift naturally. Tuulit adapts from your logs and never assumes a regular cycle or provides contraception guidance.",
                        icon = Icons.Outlined.Info
                    )

                    TuulitCard {
                        Text("What should we call you? (Optional)", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it.take(30) },
                            placeholder = { Text("First name or nickname") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input")
                        )
                    }

                    TuulitCard {
                        Text("When did your last period start?", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Choose an approximate option or skip if you don't remember.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        val options = listOf(
                            -1 to "I'm not sure / Skip",
                            0 to "Started today",
                            3 to "3 days ago",
                            10 to "10 days ago",
                            18 to "18 days ago",
                            25 to "25 days ago"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.forEach { (daysAgo, label) ->
                                FilterChip(
                                    selected = daysAgoOption == daysAgo,
                                    onClick = { daysAgoOption = daysAgo },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    TuulitCard {
                        Text("Cycle regularity", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CycleRegularity.entries.forEach { reg ->
                                FilterChip(
                                    selected = regularity == reg,
                                    onClick = { regularity = reg },
                                    label = { Text(reg.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Typical cycle length", style = MaterialTheme.typography.titleSmall)
                            Text("$cycleLength days", style = MaterialTheme.typography.labelLarge)
                        }
                        Slider(
                            value = cycleLength.toFloat(),
                            onValueChange = { cycleLength = it.roundToInt() },
                            valueRange = 21f..45f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Typical period duration", style = MaterialTheme.typography.titleSmall)
                            Text("$periodDuration days", style = MaterialTheme.typography.labelLarge)
                        }
                        Slider(
                            value = periodDuration.toFloat(),
                            onValueChange = { periodDuration = it.roundToInt() },
                            valueRange = 2f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    TuulitCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gentle local reminders", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Period estimates and quiet check-ins (discreet on lock screen).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Personalize AI with cycle context", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Optional. Off by default—change anytime in Privacy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = aiContextOptIn,
                                onCheckedChange = { aiContextOptIn = it }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TuulitSecondaryButton(
                        text = "Skip setup",
                        onClick = {
                            onComplete(displayName, null, 28, 5, CycleRegularity.NOT_SURE, notificationsEnabled, aiContextOptIn)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_skip_finish_button")
                    )
                    TuulitPrimaryButton(
                        text = "Start Tuulit",
                        onClick = {
                            val startEpoch = if (daysAgoOption >= 0) {
                                TuulitRepository.todayEpochDay() - daysAgoOption
                            } else null
                            onComplete(
                                displayName,
                                startEpoch,
                                cycleLength,
                                periodDuration,
                                regularity,
                                notificationsEnabled,
                                aiContextOptIn
                            )
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("onboarding_finish_button")
                    )
                }
            }
        }
    }
}
