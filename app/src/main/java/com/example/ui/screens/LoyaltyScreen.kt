package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerItem
import com.example.data.model.MerchantProfile
import com.example.data.model.SubscriptionPlanConfig
import com.example.domain.CopyGenerator
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoyaltyScreen(
    profile: MerchantProfile,
    activePlan: SubscriptionPlanConfig,
    customers: List<CustomerItem>,
    isBusy: Boolean,
    onAddCustomer: (name: String, phone: String, consentGranted: Boolean, notes: String) -> Unit,
    onRecordOfferSent: (customerId: String, offerTitle: String) -> Unit,
    onDeleteCustomer: (String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var offerTitle by remember { mutableStateOf("-10% Fidélité Client VIP") }
    var offerDetails by remember { mutableStateOf("Profitez de -10% sur tout le magasin ce week-end uniquement !") }

    val presets = listOf(
        "-10% Fidélité Client VIP" to "Profitez de -10% sur votre prochaine commande cette semaine !",
        "Nouvel Arrivage Exclusif" to "Nos nouveaux articles viennent d'arriver. Découvrez-les en avant-première !",
        "Offre Spéciale Livraison" to "Livraison offerte dès aujourd'hui pour nos clients fidèles."
    )

    Scaffold(
        containerColor = ObsidianBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("loyalty_add_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Ajouter un client fidèle")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fidélisation Clients",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${customers.size} / ${activePlan.maxCustomers} clients (${activePlan.name})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(text = "CONSENTEMENT", color = EmeraldSecondary)
                }
            }

            // Promo Message Builder
            item {
                GlowCard(accentColor = GoldBright) {
                    Text(
                        text = "Préparer une offre promotionnelle",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { (title, desc) ->
                            FilterChip(
                                selected = offerTitle == title,
                                onClick = {
                                    offerTitle = title
                                    offerDetails = desc
                                },
                                label = { Text(title) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = offerTitle,
                        onValueChange = { offerTitle = it },
                        label = { Text("Titre de l'offre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = offerDetails,
                        onValueChange = { offerDetails = it },
                        label = { Text("Message d'offre") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (customers.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Favorite,
                        title = "Aucun client enregistré",
                        description = "Enregistrez vos clients habituels avec leur consentement explicite pour leur envoyer vos offres spéciales.",
                        actionLabel = "Enregistrer un client",
                        onAction = { showAddDialog = true }
                    )
                }
            } else {
                items(customers, key = { it.id }) { cust ->
                    GlowCard(accentColor = EmeraldSecondary.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cust.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = "Consentement accordé",
                                        tint = EmeraldSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = cust.phone,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldPrimary
                                )
                                if (cust.lastOfferSent.isNotBlank()) {
                                    Text(
                                        text = "Dernière offre : ${cust.lastOfferSent}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteCustomer(cust.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = CoralError)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val msg = CopyGenerator.generateLoyaltyOfferMessage(
                                    businessName = profile.businessName,
                                    customerName = cust.name,
                                    offerTitle = offerTitle,
                                    offerDetails = offerDetails
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Envoyer l'offre à ${cust.name}"))
                                onRecordOfferSent(cust.id, offerTitle)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSecondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Envoyer l'offre à ${cust.name}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, consent, notes ->
                onAddCustomer(name, phone, consent, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomerDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var consentGranted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau client fidèle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom / Prénom du client *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_dialog_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numéro WhatsApp / Tél *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_dialog_phone")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Préférences (optionnel)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = consentGranted,
                        onCheckedChange = { consentGranted = it },
                        modifier = Modifier.testTag("customer_consent_checkbox")
                    )
                    Text(
                        text = "Le client a donné son consentement pour recevoir nos promotions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, consentGranted, notes) },
                enabled = !isBusy && name.isNotBlank() && phone.length >= 4 && consentGranted,
                modifier = Modifier.testTag("customer_dialog_save")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
