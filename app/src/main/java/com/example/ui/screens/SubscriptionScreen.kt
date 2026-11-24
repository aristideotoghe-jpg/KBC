package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.data.model.MerchantProfile
import com.example.data.model.PaymentProvider
import com.example.data.model.PaymentRequestItem
import com.example.data.model.PlanTier
import com.example.data.model.SubscriptionPlanConfig
import com.example.domain.CopyGenerator
import com.example.domain.GabonManualMobileMoneyGateway
import com.example.domain.PosterRenderer
import com.example.ui.components.GlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SapphireTertiary

@Composable
fun SubscriptionScreen(
    profile: MerchantProfile,
    plans: List<SubscriptionPlanConfig>,
    userPayments: List<PaymentRequestItem>,
    isBusy: Boolean,
    onSubmitPayment: (
        merchantName: String,
        merchantEmail: String,
        planId: String,
        amountFcfa: Long,
        provider: String,
        senderPhone: String,
        transactionRef: String,
        proofNote: String,
        proofImageBase64: String?
    ) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val gateway = remember { GabonManualMobileMoneyGateway() }

    val paidPlans = remember(plans) { plans.filter { it.id != PlanTier.DECOUVERTE.id } }
    var selectedPlan by remember(paidPlans) {
        mutableStateOf(paidPlans.firstOrNull { it.id == PlanTier.BUSINESS.id } ?: paidPlans.first())
    }
    var selectedProvider by remember { mutableStateOf(PaymentProvider.AIRTEL_MONEY) }
    var senderPhone by remember(profile.phone) { mutableStateOf(profile.phone ?: "") }
    var transactionRef by remember { mutableStateOf("") }
    var proofNote by remember { mutableStateOf("") }
    var proofBase64 by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = PosterRenderer.decodeUriToScaledBitmap(context, uri, maxDim = 720)
            if (bmp != null) {
                proofBase64 = PosterRenderer.bitmapToCompactBase64(bmp, maxDim = 540, quality = 65)
            }
        }
    }

    val instructions = remember(selectedProvider, selectedPlan.priceFcfa) {
        gateway.getInstructions(selectedProvider, selectedPlan.priceFcfa)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Abonnements & Paiements",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Offre actuelle : ${PlanTier.fromId(profile.plan).label}",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldPrimary
            )
        }

        // Configurable Offers
        items(plans, key = { it.id }) { plan ->
            val isCurrent = profile.plan == plan.id
            val isSelected = selectedPlan.id == plan.id
            val accent = when (plan.id) {
                "business" -> GoldPrimary
                "premium" -> EmeraldSecondary
                else -> SapphireTertiary
            }
            GlowCard(
                accentColor = if (isSelected || isCurrent) accent else MaterialTheme.colorScheme.outline,
                onClick = if (plan.id != PlanTier.DECOUVERTE.id) {
                    { selectedPlan = plan }
                } else null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium, color = accent)
                        Text(
                            text = if (plan.priceFcfa == 0L) "Gratuit" else "${CopyGenerator.formatFcfa(plan.priceFcfa)} / mois",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    when {
                        isCurrent -> StatusPill(text = "ACTIF", color = EmeraldSecondary)
                        isSelected -> StatusPill(text = "SÉLECTIONNÉ", color = GoldBright)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = plan.featuresSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Limites : ${plan.maxPostersPerMonth} affiches · ${plan.maxProducts} produits · ${plan.maxCustomers} clients",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Mobile Money Gabon Payment Instructions
        item {
            GlowCard(accentColor = GoldPrimary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = GoldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Paiement Mobile Money (Gabon) — ${selectedPlan.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentProvider.entries.forEach { prov ->
                        FilterChip(
                            selected = prov == selectedProvider,
                            onClick = { selectedProvider = prov },
                            label = { Text(prov.label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = instructions.recipientLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldBright
                )
                Text(
                    text = instructions.merchantCodeOrNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                instructions.steps.forEachIndexed { idx, step ->
                    Text(
                        text = "${idx + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CoralError, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = instructions.securityNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralError
                    )
                }
            }
        }

        // Submit Transaction Reference & Proof (NO PIN EVER)
        item {
            GlowCard(accentColor = EmeraldSecondary) {
                Text(
                    text = "Transmettre votre preuve de paiement",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = senderPhone,
                    onValueChange = { senderPhone = it },
                    label = { Text("Numéro Mobile Money émetteur *") },
                    placeholder = { Text("Ex : 074 12 34 56") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_sender_phone")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("Référence / ID de transaction *") },
                    placeholder = { Text("Ex : MP261124.1045.A88921") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_tx_ref")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = proofNote,
                    onValueChange = { proofNote = it },
                    label = { Text("Message ou précision (optionnel)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    border = BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = EmeraldSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (proofBase64 == null) "Joindre une capture du SMS/reçu (optionnel)" else "Capture jointe ✓",
                        color = EmeraldSecondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (!gateway.validateReference(transactionRef)) {
                            onMessage("Entrez une référence de transaction valide (min. 4 caractères).")
                            return@Button
                        }
                        onSubmitPayment(
                            profile.businessName,
                            profile.email,
                            selectedPlan.id,
                            selectedPlan.priceFcfa,
                            selectedProvider.id,
                            senderPhone,
                            transactionRef,
                            proofNote,
                            proofBase64
                        )
                        transactionRef = ""
                        proofNote = ""
                        proofBase64 = null
                    },
                    enabled = !isBusy && senderPhone.length >= 6 && transactionRef.length >= 4,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("payment_submit_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Envoyer pour validation (${CopyGenerator.formatFcfa(selectedPlan.priceFcfa)})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Payment Status History
        if (userPayments.isNotEmpty()) {
            item {
                Text(
                    text = "Historique de mes paiements",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(userPayments, key = { it.id }) { pay ->
                val (statusLabel, statusColor) = when (pay.status) {
                    "approved" -> "Validé" to EmeraldSecondary
                    "rejected" -> "Refusé" to CoralError
                    else -> "En attente de vérification" to GoldBright
                }
                GlowCard(accentColor = statusColor.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${PlanTier.fromId(pay.planId).label} · ${CopyGenerator.formatFcfa(pay.amountFcfa)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Réf : ${pay.transactionRef} (${PaymentProvider.fromId(pay.provider).label})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(text = statusLabel, color = statusColor)
                    }
                    if (pay.adminNote.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (pay.status == "approved") Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Note admin : ${pay.adminNote}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
