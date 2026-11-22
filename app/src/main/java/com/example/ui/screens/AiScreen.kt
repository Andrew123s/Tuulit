package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.MythVerdict
import com.example.ui.AiUiState
import com.example.ui.TuulitUiState
import com.example.ui.components.TuulitBadge
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitNoticeBanner
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.theme.TuulitThemeTokens

private enum class AiSubTab(val label: String) {
    CHAT("Ask Tuulit"),
    EXPLAIN("Explain My Body"),
    MYTHS("Myth Checker"),
    APPOINTMENT("Appointment Prep")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiScreen(
    uiState: TuulitUiState,
    aiState: AiUiState,
    onToggleAiContext: (Boolean) -> Unit,
    onSendChat: (String) -> Unit,
    onExplainBody: (String, String?) -> Unit,
    onCheckMyth: (String) -> Unit,
    onGenerateAppointmentPrep: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val extended = TuulitThemeTokens.extended
    var selectedTab by remember { mutableStateOf(AiSubTab.CHAT) }
    var chatInput by remember { mutableStateOf("") }
    var explainTopic by remember { mutableStateOf("Why do I feel bloated before my period?") }
    var mythClaim by remember { mutableStateOf("") }
    var appointmentConcern by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Tuulit AI", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Health education & wellness companion (non-diagnostic)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Honest Firebase AI Logic sample notice banner
        if (!aiState.isLiveCloudAiEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                TuulitNoticeBanner(
                    title = "Preview Mode — Firebase Required for Live AI",
                    message = "Live Gemini responses in AI Studio Android apps require enabling Firebase AI Logic. Tuulit currently provides clearly labeled sample responses.",
                    icon = Icons.Outlined.Info
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            edgePadding = 16.dp,
            divider = {}
        ) {
            AiSubTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("ai_tab_${tab.name.lowercase()}")
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            when (selectedTab) {
                AiSubTab.CHAT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // Opt-in context toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(extended.warmTan)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Include cycle context", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Optional. Shares current phase & recent symptoms only when on.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.prefs.aiContextOptIn,
                                onCheckedChange = onToggleAiContext
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(aiState.messages, key = { it.id }) { msg ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                                ) {
                                    if (!msg.safetyWarning.isNullOrBlank()) {
                                        TuulitNoticeBanner(
                                            title = "Urgent medical guidance",
                                            message = msg.safetyWarning,
                                            icon = Icons.Outlined.WarningAmber,
                                            containerColor = extended.alertSoft,
                                            accentColor = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    TuulitCard(
                                        containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        borderColor = if (msg.isUser) MaterialTheme.colorScheme.primary else extended.hairlineBorder,
                                        contentPadding = PaddingValues(14.dp),
                                        modifier = Modifier.fillMaxWidth(0.92f)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (msg.followUpPrompts.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            msg.followUpPrompts.forEach { prompt ->
                                                AssistChip(
                                                    onClick = { onSendChat(prompt) },
                                                    label = { Text(prompt, style = MaterialTheme.typography.labelMedium) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (aiState.isThinking) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text("Tuulit is thinking...", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it.take(400) },
                                placeholder = { Text("Ask about symptoms, cycle phases, sleep...") },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_chat_input")
                            )
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        onSendChat(chatInput)
                                        chatInput = ""
                                    }
                                },
                                enabled = !aiState.isThinking,
                                modifier = Modifier.testTag("ai_chat_send_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send question")
                            }
                        }
                    }
                }

                AiSubTab.EXPLAIN -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            TuulitCard {
                                Text("Explain My Body", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Clear, plain-language explanations with guided follow-ups.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = explainTopic,
                                    onValueChange = { explainTopic = it.take(200) },
                                    label = { Text("What would you like explained?") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                val quickTopics = listOf(
                                    "Why do I feel bloated before my period?",
                                    "How do period cramps work?",
                                    "Why does sleep feel lighter in the luteal phase?",
                                    "What happens during ovulation?"
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    quickTopics.forEach { t ->
                                        AssistChip(
                                            onClick = {
                                                explainTopic = t
                                                onExplainBody(t, null)
                                            },
                                            label = { Text(t, style = MaterialTheme.typography.labelMedium) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                TuulitPrimaryButton(
                                    text = "Explain This",
                                    icon = Icons.Outlined.AutoAwesome,
                                    onClick = { onExplainBody(explainTopic, null) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        aiState.explainResponse?.let { resp ->
                            item {
                                TuulitCard(containerColor = extended.warmTan) {
                                    Text(resp.text, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(12.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Explain it simply", "What can I do?", "Is this common?", "When should I worry?").forEach { style ->
                                            AssistChip(
                                                onClick = { onExplainBody(explainTopic, style) },
                                                label = { Text(style) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AiSubTab.MYTHS -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            TuulitCard {
                                Text("Health Myth Checker", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Check whether a claim about periods or reproductive health holds up.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = mythClaim,
                                    onValueChange = { mythClaim = it.take(200) },
                                    placeholder = { Text("e.g., Every normal cycle is 28 days") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "Every normal cycle is 28 days",
                                        "You shouldn't exercise on your period",
                                        "Severe period pain is always normal"
                                    ).forEach { sample ->
                                        AssistChip(
                                            onClick = {
                                                mythClaim = sample
                                                onCheckMyth(sample)
                                            },
                                            label = { Text(sample) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                TuulitPrimaryButton(
                                    text = "Check Claim",
                                    icon = Icons.Outlined.FactCheck,
                                    onClick = { onCheckMyth(mythClaim) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        aiState.mythResult?.let { res ->
                            item {
                                val badgeBg = when (res.verdict) {
                                    MythVerdict.FACT -> extended.softYellow
                                    MythVerdict.MYTH -> extended.periodClaySoft
                                    MythVerdict.NUANCED -> extended.estimateOchreSoft
                                }
                                TuulitCard {
                                    TuulitBadge(text = res.verdict.label, containerColor = badgeBg)
                                    Spacer(Modifier.height(8.dp))
                                    Text("\"${res.claim}\"", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(6.dp))
                                    Text(res.explanation, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        res.medicalNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                AiSubTab.APPOINTMENT -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            TuulitCard {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.MedicalServices, contentDescription = null)
                                    Text("Appointment Preparation", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    "Organize your real logged cycles, pain journal history, and doctor questions in one clean summary.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = appointmentConcern,
                                    onValueChange = { appointmentConcern = it.take(250) },
                                    label = { Text("Main concern or goal for your visit (optional)") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                TuulitPrimaryButton(
                                    text = "Build Health Summary",
                                    onClick = { onGenerateAppointmentPrep(appointmentConcern) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        aiState.appointmentPrep?.let { prep ->
                            item {
                                TuulitCard(containerColor = extended.warmTan) {
                                    Text(prep.title, style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Cycle Overview", style = MaterialTheme.typography.titleSmall)
                                    Text(prep.cycleOverview, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Pain History", style = MaterialTheme.typography.titleSmall)
                                    Text(prep.painSummary, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Frequent Symptoms", style = MaterialTheme.typography.titleSmall)
                                    Text(prep.symptomHighlights.joinToString(" • "), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Questions to Ask Your Clinician", style = MaterialTheme.typography.titleSmall)
                                    prep.suggestedQuestions.forEach { q ->
                                        Text("• $q", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    TuulitPrimaryButton(
                                        text = "Share / Export Summary",
                                        icon = Icons.Outlined.Share,
                                        onClick = {
                                            val text = buildString {
                                                appendLine(prep.title)
                                                appendLine()
                                                appendLine("Cycle Overview: ${prep.cycleOverview}")
                                                appendLine("Pain History: ${prep.painSummary}")
                                                appendLine("Symptoms: ${prep.symptomHighlights.joinToString(", ")}")
                                                appendLine()
                                                appendLine("Questions:")
                                                prep.suggestedQuestions.forEach { appendLine("- $it") }
                                            }
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, text)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Appointment Summary"))
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
