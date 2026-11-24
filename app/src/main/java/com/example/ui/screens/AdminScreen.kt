package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowCard
import com.example.ui.components.KpiTile
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SapphireTertiary

@Composable
fun AdminScreen(
    isAdmin: Boolean,
    merchants: List<MerchantProfile>,
    payments: List<PaymentRequestItem>,
    plans: List<SubscriptionPlanConfig>,
    isBusy: Boolean,
    onApprovePayment: (PaymentRequestItem, String) -> Unit,
    onRejectPayment: (PaymentRequestItem, String) -> Unit,
    onToggleSuspension: (MerchantProfile) -> Unit,
    onSavePlan: (SubscriptionPlanConfig) -> Unit,
    onBack: () -> Unit
) {
    if (!isAdmin) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Accès refusé — Droits administrateur requis", color = CoralError)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Retour") }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlanConfig?>(null) }

    val pendingPayments = remember(payments) { payments.filter { it.status == "pending" } }
    val collectedRevenueFcfa = remember(payments) {
        payments.filter { it.status == "approved" }.sumOf { it.amountFcfa }
    }
    val activeSubscriptions = remember(merchants) {
        merchants.count { it.plan != PlanTier.DECOUVERTE.id && it.accountStatus == "active" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldPrimary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Administration KOMBA BOOST",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Vérification serveur active",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSecondary
                    )
                }
            }
        }

        // Revenue & Stats KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        title = "Revenus encaissés",
                        value = CopyGenerator.formatFcfa(collectedRevenueFcfa),
                        subtitle = "Validés",
                        icon = Icons.Default.Payments,
                        accent = EmeraldSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        title = "Paiements en attente",
                        value = "${pendingPayments.size}",
                        subtitle = "À vérifier",
                        icon = Icons.Default.ReceiptLong,
                        accent = GoldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        title = "Commerçants",
                        value = "${merchants.size}",
                        subtitle = "Inscrits",
                        icon = Icons.Default.People,
                        accent = SapphireTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        title = "Abonnements payants",
                        value = "$activeSubscriptions",
                        subtitle = "Actifs",
                        icon = Icons.Default.AdminPanelSettings,
                        accent = GoldBright,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Paiements (${payments.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Utilisateurs (${merchants.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Offres (${plans.size})") }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (payments.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.ReceiptLong,
                            title = "Aucun paiement soumis",
                            description = "Les preuves de paiement Mobile Money apparaîtront ici pour validation manuelle."
                        )
                    }
                } else {
                    items(payments, key = { it.id }) { pay ->
                        val statusColor = when (pay.status) {
                            "approved" -> EmeraldSecondary
                            "rejected" -> CoralError
                            else -> GoldPrimary
                        }
                        GlowCard(accentColor = statusColor) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pay.merchantName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${PlanTier.fromId(pay.planId).label} · ${CopyGenerator.formatFcfa(pay.amountFcfa)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldBright
                                    )
                                    Text(
                                        text = "Réf : ${pay.transactionRef} · Tél : ${pay.senderPhone}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = PaymentProvider.fromId(pay.provider).label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SapphireTertiary
                                    )
                                }
                                if (pay.proofImageBase64 != null) {
                                    Base64Thumbnail(
                                        base64 = pay.proofImageBase64,
                                        contentDescription = "Preuve de paiement",
                                        size = 64.dp,
                                        fallbackIcon = Icons.Default.ReceiptLong
                                    )
                                }
                            }
                            if (pay.proofNote.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Note : ${pay.proofNote}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(10.dp))
                            if (pay.status == "pending") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onApprovePayment(pay, "Validé par l'administration") },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldSecondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier.weight(1f).testTag("admin_approve_payment")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Valider", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onRejectPayment(pay, "Transaction non confirmée") },
                                        enabled = !isBusy,
                                        border = BorderStroke(1.dp, CoralError),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                        modifier = Modifier.weight(1f).testTag("admin_reject_payment")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Refuser")
                                    }
                                }
                            } else {
                                StatusPill(
                                    text = if (pay.status == "approved") "Validé par ${pay.reviewedBy}" else "Refusé",
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                items(merchants, key = { it.userId }) { m ->
                    val suspended = m.accountStatus == "suspended"
                    GlowCard(accentColor = if (suspended) CoralError else EmeraldSecondary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.businessName, style = MaterialTheme.typography.titleMedium)
                                Text(m.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Offre : ${PlanTier.fromId(m.plan).label} · ${m.productsCount} produits · ${m.creationsCount} affiches",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GoldPrimary
                                )
                            }
                            StatusPill(
                                text = if (suspended) "Suspendu" else "Actif",
                                color = if (suspended) CoralError else EmeraldSecondary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { onToggleSuspension(m) },
                            enabled = !isBusy,
                            border = BorderStroke(1.dp, if (suspended) EmeraldSecondary else CoralError),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (suspended) EmeraldSecondary else CoralError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (suspended) "Réactiver ce compte" else "Suspendre ce compte")
                        }
                    }
                }
            }

            2 -> {
                items(plans, key = { it.id }) { plan ->
                    GlowCard(accentColor = GoldPrimary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.name, style = MaterialTheme.typography.titleMedium, color = GoldPrimary)
                                Text(
                                    text = "${CopyGenerator.formatFcfa(plan.priceFcfa)} / mois",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Affiches : ${plan.maxPostersPerMonth} · Produits : ${plan.maxProducts} · Textes : ${plan.maxPostsPerMonth} · Clients : ${plan.maxCustomers}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingPlan = plan }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier l'offre", tint = GoldPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    editingPlan?.let { targetPlan ->
        EditPlanDialog(
            plan = targetPlan,
            isBusy = isBusy,
            onDismiss = { editingPlan = null },
            onSave = { updated ->
                onSavePlan(updated)
                editingPlan = null
            }
        )
    }
}

@Composable
private fun EditPlanDialog(
    plan: SubscriptionPlanConfig,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlanConfig) -> Unit
) {
    var name by remember { mutableStateOf(plan.name) }
    var priceText by remember { mutableStateOf(plan.priceFcfa.toString()) }
    var maxPostersText by remember { mutableStateOf(plan.maxPostersPerMonth.toString()) }
    var maxProductsText by remember { mutableStateOf(plan.maxProducts.toString()) }
    var maxPostsText by remember { mutableStateOf(plan.maxPostsPerMonth.toString()) }
    var maxCustomersText by remember { mutableStateOf(plan.maxCustomers.toString()) }
    var summary by remember { mutableStateOf(plan.featuresSummary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurer l'offre ${plan.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom affiché") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Tarif mensuel (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxPostersText,
                        onValueChange = { maxPostersText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max affiches") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxProductsText,
                        onValueChange = { maxProductsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max produits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxPostsText,
                        onValueChange = { maxPostsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max textes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxCustomersText,
                        onValueChange = { maxCustomersText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Max clients") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Résumé fonctionnalités") },
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        plan.copy(
                            name = name.ifBlank { plan.name },
                            priceFcfa = priceText.toLongOrNull() ?: plan.priceFcfa,
                            maxPostersPerMonth = (maxPostersText.toLongOrNull() ?: plan.maxPostersPerMonth).coerceAtLeast(1L),
                            maxProducts = (maxProductsText.toLongOrNull() ?: plan.maxProducts).coerceAtLeast(1L),
                            maxPostsPerMonth = (maxPostsText.toLongOrNull() ?: plan.maxPostsPerMonth).coerceAtLeast(1L),
                            maxCustomers = (maxCustomersText.toLongOrNull() ?: plan.maxCustomers).coerceAtLeast(1L),
                            featuresSummary = summary.ifBlank { plan.featuresSummary }
                        )
                    )
                },
                enabled = !isBusy
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
