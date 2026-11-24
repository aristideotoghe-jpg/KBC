package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.SubscriptionPlanConfig
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.onGoogleSignInClicked
import com.example.domain.CopyGenerator
import com.example.ui.components.GlowCard
import com.example.ui.components.KombaLogoHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.SapphireTertiary

private data class FeatureDemo(
    val title: String,
    val description: String,
    val badge: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDemoIndex by remember { mutableStateOf(0) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val demos = remember {
        listOf(
            FeatureDemo(
                title = "Studio d'affiches Pro",
                description = "Importez la photo d'un article, ajoutez votre prix en FCFA et exportez une affiche prête pour WhatsApp Statut ou Instagram.",
                badge = "5 modèles métiers",
                icon = Icons.Default.Brush,
                accent = GoldPrimary
            ),
            FeatureDemo(
                title = "Studio de publications",
                description = "Générez des annonces commerciales percutantes en français (Direct, Chaleureux, Urgence, Luxe) et partagez-les en un geste.",
                badge = "WhatsApp · FB · IG",
                icon = Icons.AutoMirrored.Filled.Send,
                accent = EmeraldSecondary
            ),
            FeatureDemo(
                title = "Mini-Catalogue & Vitrine",
                description = "Organisez vos produits par catégories, ajustez vos prix et générez une vitrine partageable directement à vos clients.",
                badge = "Partage rapide",
                icon = Icons.Default.Inventory2,
                accent = SapphireTertiary
            ),
            FeatureDemo(
                title = "Calendrier & Fidélisation",
                description = "Programmez vos rappels de campagnes et envoyez des offres exclusives à vos clients enregistrés avec leur consentement.",
                badge = "Rappels & VIP",
                icon = Icons.Default.Favorite,
                accent = GoldBright
            )
        )
    }

    val startGoogleSignIn: () -> Unit = {
        errorMessage = null
        isLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KombaLogoHeader(subtitle = "Votre studio commercial mobile")
                    StatusPill(text = "GABON · FCFA", color = EmeraldSecondary)
                }
            }

            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it / 6 }
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.komba_hero_banner_1795522759285),
                                    contentDescription = "Studio KOMBA BOOST",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, ObsidianCard.copy(alpha = 0.96f))
                                            )
                                        )
                                )
                                StatusPill(
                                    text = "CONÇU POUR LES COMMERÇANTS",
                                    color = GoldBright,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                                Text(
                                    text = "Vendez plus avec des visuels et messages professionnels.",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Créez vos affiches en FCFA, rédigez vos annonces WhatsApp & Facebook, organisez votre catalogue et fidélisez vos clients depuis votre téléphone Android.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))

                                if (errorMessage != null) {
                                    Surface(
                                        color = CoralError.copy(alpha = 0.14f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = errorMessage!!,
                                            color = CoralError,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = startGoogleSignIn,
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("google_signup_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldPrimary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Inscription Google", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = startGoogleSignIn,
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("google_signin_button"),
                                        border = BorderStroke(1.dp, EmeraldSecondary.copy(alpha = 0.7f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSecondary)
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Connexion", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Démonstration des fonctionnalités
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Démonstration des outils",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(demos.indices.toList()) { idx ->
                            val demo = demos[idx]
                            val selected = idx == selectedDemoIndex
                            Surface(
                                onClick = { selectedDemoIndex = idx },
                                shape = RoundedCornerShape(50),
                                color = if (selected) demo.accent.copy(alpha = 0.2f) else ObsidianCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) demo.accent else ObsidianOutline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        demo.icon,
                                        contentDescription = demo.title,
                                        tint = if (selected) demo.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = demo.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) demo.accent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    val activeDemo = demos[selectedDemoIndex]
                    GlowCard(accentColor = activeDemo.accent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    activeDemo.icon,
                                    contentDescription = null,
                                    tint = activeDemo.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = activeDemo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            StatusPill(text = activeDemo.badge, color = activeDemo.accent)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = activeDemo.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tarifs transparents
            item {
                Text(
                    text = "Tarifs simples et transparents",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(SubscriptionPlanConfig.defaults()) { plan ->
                val accent = when (plan.id) {
                    "business" -> GoldPrimary
                    "premium" -> EmeraldSecondary
                    else -> SapphireTertiary
                }
                GlowCard(accentColor = accent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = plan.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = accent
                            )
                            Text(
                                text = if (plan.priceFcfa == 0L) "Gratuit pour démarrer" else "${CopyGenerator.formatFcfa(plan.priceFcfa)} / mois",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (plan.id == "business") {
                            StatusPill(text = "POPULAIRE", color = GoldPrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = plan.featuresSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section Témoignages (policy: réels uniquement)
            item {
                GlowCard(accentColor = ObsidianOutline) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Retours des commerçants",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Les témoignages vérifiés des commerçants pilotes KOMBA BOOST seront publiés ici au fur et à mesure des retours réels sur le terrain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
