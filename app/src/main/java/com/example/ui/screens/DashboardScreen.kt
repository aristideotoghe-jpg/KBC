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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessCategory
import com.example.data.model.MerchantProfile
import com.example.data.model.PlanTier
import com.example.data.model.SubscriptionPlanConfig
import com.example.ui.components.GlowCard
import com.example.ui.components.KombaLogoHeader
import com.example.ui.components.KpiTile
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SapphireTertiary
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    profile: MerchantProfile,
    activePlan: SubscriptionPlanConfig,
    productsCount: Int,
    postersCount: Int,
    postsCount: Int,
    customersCount: Int,
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val categoryLabel = BusinessCategory.fromId(profile.businessCategory).label
    val planTier = PlanTier.fromId(profile.plan)
    val renewalText = profile.renewalDate?.toDate()?.let { date ->
        SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(date)
    } ?: if (planTier == PlanTier.DECOUVERTE) "Offre gratuite sans expiration" else "Actif — en cours"

    val steps = listOf(
        "Compte créé" to true,
        "Activité choisie ($categoryLabel)" to profile.businessName.isNotBlank(),
        "Premier produit ajouté" to (productsCount > 0 || profile.productsCount > 0),
        "Première affiche créée" to (postersCount > 0 || profile.creationsCount > 0),
        "Publicité partagée" to (profile.sharesCount > 0)
    )
    val completedSteps = steps.count { it.second }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KombaLogoHeader(subtitle = "Bonjour, ${profile.businessName}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        IconButton(
                            onClick = { onNavigate("admin") },
                            modifier = Modifier.testTag("dashboard_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Espace Administrateur",
                                tint = GoldPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("dashboard_signout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Déconnexion",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Subscription & Renewal Banner
        item {
            val planColor = when (planTier) {
                PlanTier.DECOUVERTE -> SapphireTertiary
                PlanTier.BUSINESS -> GoldPrimary
                PlanTier.PREMIUM -> EmeraldSecondary
            }
            GlowCard(accentColor = planColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Abonnement actuel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = activePlan.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = planColor
                        )
                        Text(
                            text = "Renouvellement : $renewalText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { onNavigate("subscription") },
                        border = BorderStroke(1.dp, planColor.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = planColor),
                        modifier = Modifier.testTag("dashboard_upgrade_button")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (planTier == PlanTier.DECOUVERTE) "Booster" else "Gérer")
                    }
                }
            }
        }

        // 2x2 KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        title = "Affiches créées",
                        value = "${maxOf(postersCount.toLong(), profile.creationsCount)}",
                        subtitle = "Max ${activePlan.maxPostersPerMonth}",
                        icon = Icons.Default.Brush,
                        accent = GoldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        title = "Publications",
                        value = "${maxOf(postsCount.toLong(), profile.postsCount)}",
                        subtitle = "Max ${activePlan.maxPostsPerMonth}",
                        icon = Icons.AutoMirrored.Filled.Send,
                        accent = EmeraldSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        title = "Produits catalogue",
                        value = "$productsCount",
                        subtitle = "Max ${activePlan.maxProducts}",
                        icon = Icons.Default.Inventory2,
                        accent = SapphireTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        title = "Clients fidèles",
                        value = "$customersCount",
                        subtitle = "Max ${activePlan.maxCustomers}",
                        icon = Icons.Default.Favorite,
                        accent = GoldBright,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Guided 5-step progress
        item {
            GlowCard(accentColor = EmeraldSecondary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Votre démarrage guidé",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusPill(text = "$completedSteps / ${steps.size}", color = EmeraldSecondary)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { completedSteps.toFloat() / steps.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = EmeraldSecondary
                )
                Spacer(Modifier.height(10.dp))
                steps.forEach { (label, done) ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (done) EmeraldSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Tools Grid
        item {
            Text(
                text = "Outils marketing & vente",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Studio d'affiches",
                        subtitle = "Créer & partager",
                        icon = Icons.Default.Brush,
                        accent = GoldPrimary,
                        onClick = { onNavigate("posters") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_posters")
                    )
                    QuickActionTile(
                        title = "Studio de textes",
                        subtitle = "WhatsApp · FB · IG",
                        icon = Icons.AutoMirrored.Filled.Send,
                        accent = EmeraldSecondary,
                        onClick = { onNavigate("posts") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_posts")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Calendrier",
                        subtitle = "Campagnes & rappels",
                        icon = Icons.Default.CalendarMonth,
                        accent = SapphireTertiary,
                        onClick = { onNavigate("calendar") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_calendar")
                    )
                    QuickActionTile(
                        title = "Fidélisation",
                        subtitle = "Clients & promos",
                        icon = Icons.Default.Favorite,
                        accent = GoldBright,
                        onClick = { onNavigate("loyalty") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_loyalty")
                    )
                }
            }
        }

        if (isAdmin) {
            item {
                Button(
                    onClick = { onNavigate("admin") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary.copy(alpha = 0.18f),
                        contentColor = GoldBright
                    ),
                    border = BorderStroke(1.dp, GoldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_entry_button")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ouvrir l'Espace Administration", fontWeight = FontWeight.Bold)
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = ObsidianCard,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
