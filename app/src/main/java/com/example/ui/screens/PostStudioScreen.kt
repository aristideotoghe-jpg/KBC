package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CopyStyle
import com.example.data.model.MerchantProfile
import com.example.data.model.ProductItem
import com.example.data.model.SocialPlatform
import com.example.data.model.SocialPostItem
import com.example.data.model.SubscriptionPlanConfig
import com.example.domain.CopyGenerator
import com.example.ui.components.GlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.SapphireTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostStudioScreen(
    profile: MerchantProfile,
    activePlan: SubscriptionPlanConfig,
    products: List<ProductItem>,
    posts: List<SocialPostItem>,
    isBusy: Boolean,
    onSavePost: (productName: String, priceFcfa: Long, platform: String, style: String, content: String) -> Unit,
    onShareRecorded: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var productName by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf(SocialPlatform.WHATSAPP) }
    var style by remember { mutableStateOf(CopyStyle.DIRECT) }
    var generatedText by remember { mutableStateOf("") }

    val priceLong = priceText.filter { it.isDigit() }.toLongOrNull() ?: 0L

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
                        text = "Studio de publications",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${posts.size} / ${activePlan.maxPostsPerMonth} textes (${activePlan.name})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(text = "MODÈLES PRO", color = EmeraldSecondary)
            }
        }

        // Honest integration notice (never pretend auto-publishing)
        item {
            GlowCard(accentColor = SapphireTertiary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SapphireTertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Préparez vos textes commerciaux, copiez-les ou partagez-les directement vers WhatsApp, Facebook ou Instagram.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (products.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products, key = { it.id }) { prod ->
                        Surface(
                            onClick = {
                                productName = prod.name
                                priceText = prod.priceFcfa.toString()
                                details = prod.description
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

        item {
            GlowCard(accentColor = GoldPrimary) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nom du produit ou de l'offre *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_product_name")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Prix (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Atout principal / Détails") },
                    placeholder = { Text("Livraison gratuite, stock limité, paiement Airtel Money...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Réseau cible", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialPlatform.entries.forEach { p ->
                        FilterChip(
                            selected = p == platform,
                            onClick = { platform = p },
                            label = { Text(p.label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Style de rédaction", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CopyStyle.entries.forEach { s ->
                        FilterChip(
                            selected = s == style,
                            onClick = { style = s },
                            label = { Text(s.label) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (productName.isBlank()) {
                            onMessage("Saisissez d'abord le nom du produit.")
                            return@Button
                        }
                        generatedText = CopyGenerator.generateCommercialPost(
                            businessName = profile.businessName,
                            productName = productName,
                            priceFcfa = priceLong,
                            benefitOrDetails = details,
                            contactPhone = profile.phone ?: "",
                            platform = platform,
                            style = style
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("post_generate_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Générer le texte commercial", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (generatedText.isNotBlank()) {
            item {
                GlowCard(accentColor = EmeraldSecondary) {
                    Text("Texte généré (${platform.label})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = generatedText,
                        onValueChange = { generatedText = it },
                        minLines = 5,
                        maxLines = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_generated_editor")
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(generatedText))
                                onMessage("Texte copié dans le presse-papiers !")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("post_copy_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copier")
                        }
                        OutlinedButton(
                            onClick = {
                                onSavePost(productName, priceLong, platform.id, style.id, generatedText)
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("post_save_button")
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sauver")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, generatedText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Partager vers ${platform.label}"))
                            onShareRecorded()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSecondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("post_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Partager vers une application", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (posts.isNotEmpty()) {
            item {
                Text(
                    text = "Publications enregistrées (${posts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(posts, key = { it.id }) { item ->
                GlowCard(
                    onClick = {
                        generatedText = item.content
                        productName = item.productName
                        priceText = item.priceFcfa.toString()
                        platform = SocialPlatform.fromId(item.platform)
                        style = CopyStyle.fromId(item.style)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = EmeraldSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(item.productName, style = MaterialTheme.typography.titleMedium)
                        }
                        StatusPill(text = SocialPlatform.fromId(item.platform).label, color = SapphireTertiary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
