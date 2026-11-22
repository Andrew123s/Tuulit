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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.TrustedContactEntity
import com.example.domain.BananaModePersona
import com.example.domain.CycleRegularity
import com.example.ui.TuulitUiState
import com.example.ui.components.BananaMascot
import com.example.ui.components.TuulitCard
import com.example.ui.components.TuulitNoticeBanner
import com.example.ui.components.TuulitPrimaryButton
import com.example.ui.components.TuulitSecondaryButton
import com.example.ui.theme.TuulitThemeTokens
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    uiState: TuulitUiState,
    onUpdateProfile: (String, Int, Int, CycleRegularity) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleAiContext: (Boolean) -> Unit,
    onUpdateNotifications: (Boolean, Boolean, Boolean, Boolean, Boolean, String) -> Unit,
    onSetPrivatePin: (String) -> Unit,
    onExportCsv: () -> String,
    onWipeAllData: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenPainJournal: () -> Unit,
    onOpenConnect: () -> Unit,
) {
    val context = LocalContext.current
    val extended = TuulitThemeTokens.extended
    val prefs = uiState.prefs

    var editName by remember(prefs.displayName) { mutableStateOf(prefs.displayName) }
    var editRegularity by remember(prefs.cycleRegularity) { mutableStateOf(prefs.cycleRegularity) }
    var pinInput by remember { mutableStateOf("") }
    var showWipeConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Profile & Privacy", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Full control over your cycle settings, alerts, and personal data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BananaMascot(persona = prefs.bananaMode, size = 56.dp)
                }
            }

            // Shortcuts to Insights, Pain Journal, Tuulit Connect
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TuulitCard(onClick = onOpenInsights, modifier = Modifier.testTag("profile_insights_row")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Insights, contentDescription = null)
                            Column {
                                Text("Insights & Cycle Mood Map", style = MaterialTheme.typography.titleMedium)
                                Text("Trends, history charts, and symptom frequency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    TuulitCard(onClick = onOpenPainJournal, modifier = Modifier.testTag("profile_pain_journal_row")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null)
                            Column {
                                Text("Pain Journal", style = MaterialTheme.typography.titleMedium)
                                Text("Detailed pain history & pattern detection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    TuulitCard(onClick = onOpenConnect, modifier = Modifier.testTag("profile_connect_row")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Group, contentDescription = null)
                            Column {
                                Text("Tuulit Connect & Couple Mode", style = MaterialTheme.typography.titleMedium)
                                Text("Consent-based trusted support & partner education", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Personalization & Cycle Defaults
            item {
                TuulitCard {
                    Text("Personal Information & Cycle Defaults", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.take(30) },
                        label = { Text("Display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Cycle regularity", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CycleRegularity.entries.forEach { reg ->
                            FilterChip(
                                selected = editRegularity == reg,
                                onClick = { editRegularity = reg },
                                label = { Text(reg.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    TuulitPrimaryButton(
                        text = "Save Profile",
                        onClick = {
                            onUpdateProfile(editName, prefs.defaultCycleLength, prefs.defaultPeriodDuration, editRegularity)
                        }
                    )
                }
            }

            // Notifications & Quiet Hours
            item {
                TuulitCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null)
                        Text("Notifications & Quiet Hours", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Enable reminders",
                        subtitle = "Quiet hours: ${prefs.quietHoursLabel}",
                        checked = prefs.notificationsEnabled,
                        onCheckedChange = {
                            onUpdateNotifications(it, prefs.notifyPeriodApproaching, prefs.notifyDailyCheckIn, prefs.notifyCareReminders, prefs.hideLockScreenDetails, prefs.quietHoursLabel)
                        }
                    )
                    SettingSwitchRow(
                        title = "Period estimate reminders",
                        subtitle = "Gentle heads-up when your estimated window nears",
                        checked = prefs.notifyPeriodApproaching,
                        onCheckedChange = {
                            onUpdateNotifications(prefs.notificationsEnabled, it, prefs.notifyDailyCheckIn, prefs.notifyCareReminders, prefs.hideLockScreenDetails, prefs.quietHoursLabel)
                        }
                    )
                    SettingSwitchRow(
                        title = "Daily check-in prompt",
                        subtitle = "Never sends streak-loss or guilt alerts",
                        checked = prefs.notifyDailyCheckIn,
                        onCheckedChange = {
                            onUpdateNotifications(prefs.notificationsEnabled, prefs.notifyPeriodApproaching, it, prefs.notifyCareReminders, prefs.hideLockScreenDetails, prefs.quietHoursLabel)
                        }
                    )
                    SettingSwitchRow(
                        title = "Hide sensitive details on lock screen",
                        subtitle = "Shows neutral copy like 'Take a quick moment for yourself'",
                        checked = prefs.hideLockScreenDetails,
                        onCheckedChange = {
                            onUpdateNotifications(prefs.notificationsEnabled, prefs.notifyPeriodApproaching, prefs.notifyDailyCheckIn, prefs.notifyCareReminders, it, prefs.quietHoursLabel)
                        }
                    )
                }
            }

            // Private Mode & AI Privacy
            item {
                TuulitCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Shield, contentDescription = null)
                        Text("Privacy & Private Mode", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "All cycle logs, symptoms, pain entries, and journals are currently stored locally on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    SettingSwitchRow(
                        title = "Share cycle context with Tuulit AI",
                        subtitle = "Optional opt-in for personalized educational answers",
                        checked = prefs.aiContextOptIn,
                        onCheckedChange = onToggleAiContext
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (prefs.privateModePin.isBlank()) "App PIN lock is currently off" else "App PIN lock is active (4+ digits)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                            label = { Text("Set 4–6 digit PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TuulitPrimaryButton(
                            text = "Save",
                            enabled = pinInput.length >= 4,
                            onClick = {
                                onSetPrivatePin(pinInput)
                                pinInput = ""
                            }
                        )
                    }
                    if (prefs.privateModePin.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { onSetPrivatePin("") }) {
                            Text("Remove PIN lock")
                        }
                    }
                }
            }

            // Theme & Accessibility
            item {
                TuulitCard {
                    Text("Appearance & Accessibility", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    SettingSwitchRow(
                        title = "Warm Dark Mode",
                        subtitle = "Deep charcoal surfaces with warm cream ink",
                        checked = prefs.darkThemeEnabled,
                        onCheckedChange = onToggleDarkTheme
                    )
                    SettingSwitchRow(
                        title = "Reduced motion",
                        subtitle = "Minimizes breathing and transition animations",
                        checked = prefs.reducedMotionEnabled,
                        onCheckedChange = onToggleReducedMotion
                    )
                }
            }

            // Data Export & Permanent Deletion
            item {
                TuulitCard {
                    Text("Your Data", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    TuulitSecondaryButton(
                        text = "Export All Records (CSV)",
                        icon = Icons.Outlined.FileDownload,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Tuulit Health Export")
                                putExtra(Intent.EXTRA_TEXT, onExportCsv())
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Tuulit Data"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    TuulitSecondaryButton(
                        text = "Delete All Local Data",
                        icon = Icons.Outlined.DeleteForever,
                        onClick = { showWipeConfirm = true },
                        modifier = Modifier.fillMaxWidth().testTag("profile_delete_all_data_button")
                    )
                }
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Delete all Tuulit data?") },
            text = { Text("This permanently deletes all cycle history, daily logs, pain entries, journal notes, survival kit items, and settings on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showWipeConfirm = false
                    onWipeAllData()
                }) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ConnectScreen(
    uiState: TuulitUiState,
    onSaveContact: (TrustedContactEntity) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val extended = TuulitThemeTokens.extended

    var contactName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Partner") }
    var showPartnerPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("connect_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Tuulit Connect & Couple Mode", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Consent-first support with people you trust.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TuulitNoticeBanner(
                    title = "Local Sharing Mode",
                    message = "Live multi-device syncing requires enabling Firebase Cloud Firestore. Today you can manage trusted contacts, set exact sharing permissions, preview the Partner Experience, and share invites or Tuulit SOS updates via your messaging apps.",
                    icon = Icons.Outlined.Info
                )
            }

            // Add Trusted Contact
            item {
                TuulitCard {
                    Text("Invite a Trusted Person", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it.take(30) },
                        label = { Text("Name (partner, friend, sister, family)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it.take(24) },
                        label = { Text("Relationship") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    TuulitPrimaryButton(
                        text = "Create Invite & Add Contact",
                        icon = Icons.Outlined.Add,
                        onClick = {
                            if (contactName.isNotBlank()) {
                                val code = "TUULIT-${Random.nextInt(1000, 9999)}"
                                onSaveContact(
                                    TrustedContactEntity(
                                        name = contactName.trim(),
                                        relationship = relationship.trim().ifBlank { "Trusted Person" },
                                        inviteCode = code
                                    )
                                )
                                contactName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("connect_add_contact_button")
                    )
                }
            }

            // Existing Connected Contacts with Granular Sharing Toggles + Instant Revoke
            items(uiState.trustedContacts, key = { it.id }) { contact ->
                TuulitCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                            Text("${contact.relationship} • Invite ${contact.inviteCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteContact(contact.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Revoke access")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    SettingSwitchRow(
                        title = "Period approaching",
                        subtitle = "High-level heads-up only",
                        checked = contact.sharePeriodApproaching,
                        onCheckedChange = { onSaveContact(contact.copy(sharePeriodApproaching = it)) }
                    )
                    SettingSwitchRow(
                        title = "Period ongoing",
                        subtitle = "Shows supportive check-in cues",
                        checked = contact.sharePeriodOngoing,
                        onCheckedChange = { onSaveContact(contact.copy(sharePeriodOngoing = it)) }
                    )
                    SettingSwitchRow(
                        title = "General cycle phase",
                        subtitle = "Optional",
                        checked = contact.shareCyclePhase,
                        onCheckedChange = { onSaveContact(contact.copy(shareCyclePhase = it)) }
                    )
                    SettingSwitchRow(
                        title = "Selected moods & symptoms",
                        subtitle = "Never shared unless toggled on",
                        checked = contact.shareMoods,
                        onCheckedChange = { onSaveContact(contact.copy(shareMoods = it, shareSymptoms = it)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    TuulitSecondaryButton(
                        text = "Share Invite / Permitted Update",
                        icon = Icons.Outlined.Share,
                        onClick = {
                            val summary = buildString {
                                appendLine("Hi ${contact.name}, I added you to my Tuulit support circle (Invite ${contact.inviteCode}).")
                                if (contact.sharePeriodOngoing && uiState.prediction.isCurrentlyMenstruating) {
                                    appendLine("Update: My period is ongoing right now—extra comfort is welcome.")
                                } else if (contact.sharePeriodApproaching) {
                                    appendLine("Update: ${uiState.prediction.estimateSummary}")
                                }
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summary)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share with ${contact.name}"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Partner Experience Preview & Couple Mode Guide
            item {
                TuulitCard(containerColor = extended.warmTan) {
                    Text("Couple Mode & Partner Experience", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Preview what a supportive partner sees—designed to encourage kindness without monitoring or control.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    TuulitSecondaryButton(
                        text = if (showPartnerPreview) "Hide Partner Preview" else "Preview Partner View",
                        onClick = { showPartnerPreview = !showPartnerPreview }
                    )
                    if (showPartnerPreview) {
                        Spacer(Modifier.height(12.dp))
                        TuulitCard {
                            Text("Your person may appreciate a little extra support today.", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Thoughtful ideas:", style = MaterialTheme.typography.labelLarge)
                            Text("• Ask how they are feeling rather than assuming", style = MaterialTheme.typography.bodySmall)
                            Text("• Offer a warm drink, snack, or heating pad", style = MaterialTheme.typography.bodySmall)
                            Text("• Help out with a chore or plan a quiet evening", style = MaterialTheme.typography.bodySmall)
                            Text("• Give space and patience if they feel low-energy", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateModeLockScreen(
    onSubmitPin: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        TuulitCard(modifier = Modifier.widthIn(max = 420.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BananaMascot(persona = BananaModePersona.NEUTRAL, size = 72.dp)
                Icon(Icons.Outlined.Lock, contentDescription = "Private Mode locked")
                Text("Tuulit Private Mode", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Enter your PIN to access your cycle logs and journal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) {
                            pin = it
                            errorMsg = null
                        }
                    },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("private_mode_pin_input")
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                TuulitPrimaryButton(
                    text = "Unlock Tuulit",
                    onClick = {
                        if (!onSubmitPin(pin)) {
                            errorMsg = "Incorrect PIN. Please try again."
                            pin = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("private_mode_unlock_button")
                )
            }
        }
    }
}
