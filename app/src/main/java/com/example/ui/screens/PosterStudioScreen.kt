package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MerchantProfile
import com.example.data.model.PlanTier
import com.example.data.model.PosterItem
import com.example.data.model.PosterTemplate
import com.example.data.model.ProductItem
import com.example.data.model.SubscriptionPlanConfig
import com.example.domain.CopyGenerator
import com.example.domain.PosterRenderer
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.GlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline

@Composable
fun PosterStudioScreen(
    profile: MerchantProfile,
    activePlan: SubscriptionPlanConfig,
    products: List<ProductItem>,
    posters: List<PosterItem>,
    isBusy: Boolean,
    onSavePoster: (
        productName: String,
        priceFcfa: Long,
        description: String,
        templateId: String,
        promoBadge: String,
        contactPhone: String,
        imageBase64: String?
    ) -> Unit,
    onShareRecorded: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var promoBadge by remember { mutableStateOf("NOUVEAU") }
    var contactPhone by remember(profile.phone) { mutableStateOf(profile.phone ?: "") }
    var selectedTemplate by remember { mutableStateOf(PosterTemplate.VETEMENTS_LUXE) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedBitmap = PosterRenderer.decodeUriToScaledBitmap(context, uri)
        }
    }

    val priceLong = priceText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val showWatermark = profile.plan == PlanTier.DECOUVERTE.id

    val renderedPoster = remember(
        profile.businessName,
        productName,
        priceLong,
        description,
        promoBadge,
        contactPhone,
        selectedTemplate,
        selectedBitmap,
        showWatermark
    ) {
        PosterRenderer.renderPosterBitmap(
            businessName = profile.businessName,
            productName = productName,
            priceFcfa = priceLong,
            description = description,
            promoBadge = promoBadge,
            contactPhone = contactPhone,
            template = selectedTemplate,
            productImage = selectedBitmap,
            showWatermark = showWatermark
        )
    }

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
                Column {
                    Text(
                        text = "Studio d'affiches",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${posters.size} / ${activePlan.maxPostersPerMonth} affiches (${activePlan.name})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(text = "PNG HD", color = GoldPrimary)
            }
        }

        // Quick import from Catalogue
        if (products.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Importer depuis votre catalogue",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(products, key = { it.id }) { prod ->
                            Surface(
                                onClick = {
                                    productName = prod.name
                                    priceText = prod.priceFcfa.toString()
                                    description = prod.description
                                    selectedBitmap = PosterRenderer.base64ToBitmap(prod.imageBase64)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = ObsidianCard,
                                border = BorderStroke(1.dp, ObsidianOutline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(prod.name, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Photo Picker + Fields
        item {
            GlowCard(accentColor = GoldPrimary) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("poster_pick_photo_button"),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = GoldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedBitmap == null) "Importer une photo du téléphone" else "Changer la photo du produit",
                        color = GoldPrimary
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nom du produit *") },
                    placeholder = { Text("Ex : Ensemble Wax Premium") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poster_product_name")
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { input -> priceText = input.filter { it.isDigit() } },
                        label = { Text("Prix (FCFA) *") },
                        placeholder = { Text("15000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("poster_price")
                    )
                    OutlinedTextField(
                        value = promoBadge,
                        onValueChange = { promoBadge = it.take(24) },
                        label = { Text("Badge promo") },
                        placeholder = { Text("-20% / VIP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("Description courte") },
                    placeholder = { Text("Tailles S à XL, livraison sur Libreville") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poster_description")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it.take(30) },
                    label = { Text("Téléphone / WhatsApp affiché") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Template Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Modèle graphique par métier",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PosterTemplate.entries) { tpl ->
                        val selected = tpl == selectedTemplate
                        FilterChip(
                            selected = selected,
                            onClick = { selectedTemplate = tpl },
                            label = { Text("${tpl.label} (${tpl.categoryHint})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(alpha = 0.22f),
                                selectedLabelColor = GoldPrimary
                            )
                        )
                    }
                }
            }
        }

        // Live Rendered Poster Preview
        item {
            GlowCard(accentColor = EmeraldSecondary) {
                Text(
                    text = "Aperçu de l'affiche générée",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1080f / 1350f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianCard)
                ) {
                    Image(
                        bitmap = renderedPoster.asImageBitmap(),
                        contentDescription = "Aperçu affiche publicitaire",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (productName.isBlank()) {
                            onMessage("Indiquez le nom du produit avant d'enregistrer.")
                            return@Button
                        }
                        val base64 = selectedBitmap?.let { PosterRenderer.bitmapToCompactBase64(it) }
                        onSavePoster(
                            productName,
                            priceLong,
                            description,
                            selectedTemplate.id,
                            promoBadge,
                            contactPhone,
                            base64
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("poster_save_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enregistrer l'affiche", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            PosterRenderer.savePosterToGallery(context, renderedPoster, productName)
                                .onSuccess { onMessage(it) }
                                .onFailure { onMessage("Erreur téléchargement : ${it.localizedMessage}") }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("poster_download_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Télécharger")
                    }

                    Button(
                        onClick = {
                            val caption = "${productName.ifBlank { "Nouveau produit" }} — ${CopyGenerator.formatFcfa(priceLong)}\n${profile.businessName}"
                            PosterRenderer.sharePosterBitmap(context, renderedPoster, caption)
                            onShareRecorded()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSecondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("poster_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Partager", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Saved Posters History
        if (posters.isNotEmpty()) {
            item {
                Text(
                    text = "Mes dernières affiches (${posters.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(posters, key = { it.id }) { poster ->
                GlowCard(
                    onClick = {
                        productName = poster.productName
                        priceText = poster.priceFcfa.toString()
                        description = poster.description
                        promoBadge = poster.promoBadge
                        contactPhone = poster.contactPhone
                        selectedTemplate = PosterTemplate.fromId(poster.templateId)
                        selectedBitmap = PosterRenderer.base64ToBitmap(poster.imageBase64)
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Base64Thumbnail(
                            base64 = poster.imageBase64,
                            contentDescription = poster.productName,
                            fallbackIcon = Icons.Default.Brush
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = poster.productName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CopyGenerator.formatFcfa(poster.priceFcfa),
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldPrimary
                            )
                            Text(
                                text = PosterTemplate.fromId(poster.templateId).label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
