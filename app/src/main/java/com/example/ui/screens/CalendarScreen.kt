package com.example.ui.screens

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CalendarEventItem
import com.example.data.model.SocialPlatform
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SapphireTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    events: List<CalendarEventItem>,
    isBusy: Boolean,
    onAddEvent: (title: String, platform: String, campaignType: String, scheduledEpochMs: Long, notes: String) -> Unit,
    onMarkStatus: (eventId: String, status: String) -> Unit,
    onDeleteEvent: (eventId: String) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = SapphireTertiary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("calendar_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Planifier une campagne")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Calendrier & Campagnes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Organisez vos publications et programmez des rappels sur votre téléphone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (events.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.CalendarMonth,
                        title = "Aucune campagne planifiée",
                        description = "Préparez vos lancements du week-end, promos de fin de mois ou arrivages produits.",
                        actionLabel = "Planifier maintenant",
                        onAction = { showDialog = true }
                    )
                }
            } else {
                items(events, key = { it.id }) { ev ->
                    val dateText = remember(ev.scheduledDateEpochMs) {
                        SimpleDateFormat("EEE dd MMM · HH:mm", Locale.FRENCH)
                            .format(Date(ev.scheduledDateEpochMs))
                    }
                    val isDone = ev.status == "done"
                    GlowCard(accentColor = if (isDone) EmeraldSecondary else SapphireTertiary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "$dateText · ${SocialPlatform.fromId(ev.platform).label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary
                                )
                            }
                            StatusPill(
                                text = if (isDone) "Publié" else ev.campaignType,
                                color = if (isDone) EmeraldSecondary else SapphireTertiary
                            )
                        }
                        if (ev.notes.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = ev.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                        data = CalendarContract.Events.CONTENT_URI
                                        putExtra(CalendarContract.Events.TITLE, "KOMBA BOOST : ${ev.title}")
                                        putExtra(CalendarContract.Events.DESCRIPTION, "${ev.campaignType} (${ev.platform}) - ${ev.notes}")
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ev.scheduledDateEpochMs)
                                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ev.scheduledDateEpochMs + 30 * 60 * 1000L)
                                    }
                                    context.startActivity(calIntent)
                                    onMarkStatus(ev.id, "reminded")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Rappel")
                            }
                            if (!isDone) {
                                OutlinedButton(
                                    onClick = { onMarkStatus(ev.id, "done") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Fait")
                                }
                            }
                            IconButton(onClick = { onDeleteEvent(ev.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = CoralError)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showDialog) {
        AddCalendarDialog(
            isBusy = isBusy,
            onDismiss = { showDialog = false },
            onSave = { title, plat, type, epochMs, notes ->
                onAddEvent(title, plat, type, epochMs, notes)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCalendarDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(SocialPlatform.WHATSAPP) }
    var campaignType by remember { mutableStateOf("Promo Week-end") }
    var daysFromNow by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle campagne") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la publication *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("calendar_dialog_title")
                )
                Text("Plateforme", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SocialPlatform.entries.forEach { p ->
                        FilterChip(
                            selected = p == platform,
                            onClick = { platform = p },
                            label = { Text(p.label) }
                        )
                    }
                }
                Text("Quand ?", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Aujourd'hui", 1 to "Demain", 3 to "+3 jours", 7 to "+1 semaine").forEach { (d, lbl) ->
                        FilterChip(
                            selected = daysFromNow == d,
                            onClick = { daysFromNow = d },
                            label = { Text(lbl) }
                        )
                    }
                }
                OutlinedTextField(
                    value = campaignType,
                    onValueChange = { campaignType = it },
                    label = { Text("Type de campagne") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Produit visé") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetMs = System.currentTimeMillis() + (daysFromNow * 24L * 60L * 60L * 1000L) + 3_600_000L
                    onSave(title, platform.id, campaignType, targetMs, notes)
                },
                enabled = !isBusy && title.isNotBlank()
            ) {
                Text("Planifier")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
