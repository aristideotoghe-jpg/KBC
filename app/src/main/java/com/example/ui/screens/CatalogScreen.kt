package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessCategory
import com.example.data.model.MerchantProfile
import com.example.data.model.ProductItem
import com.example.data.model.SubscriptionPlanConfig
import com.example.domain.CopyGenerator
import com.example.domain.PosterRenderer
import com.example.ui.components.Base64Thumbnail
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GlowCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg

@Composable
fun CatalogScreen(
    profile: MerchantProfile,
    activePlan: SubscriptionPlanConfig,
    products: List<ProductItem>,
    isBusy: Boolean,
    onSaveProduct: (
        productId: String?,
        name: String,
        priceFcfa: Long,
        category: String,
        description: String,
        imageBase64: String?,
        available: Boolean
    ) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onShareRecorded: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("Tout") }
    var editingProduct by remember { mutableStateOf<ProductItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("Tout") + products.map { it.category }.filter { it.isNotBlank() }.distinct()
    }
    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "Tout") products else products.filter { it.category == selectedCategory }
    }

    Scaffold(
        containerColor = ObsidianBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddDialog = true
                },
                containerColor = GoldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("catalog_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un produit")
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
                            text = "Mini-Catalogue",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${products.size} / ${activePlan.maxProducts} produits (${activePlan.name})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val vitrine = CopyGenerator.generateStorefrontText(
                                businessName = profile.businessName,
                                categoryLabel = BusinessCategory.fromId(profile.businessCategory).label,
                                phone = profile.phone,
                                products = products
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, vitrine)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager ma vitrine"))
                            onShareRecorded()
                        },
                        border = BorderStroke(1.dp, EmeraldSecondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSecondary),
                        modifier = Modifier.testTag("catalog_share_vitrine")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Vitrine")
                    }
                }
            }

            if (categories.size > 1) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = cat == selectedCategory,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Inventory2,
                        title = "Votre catalogue est vide",
                        description = "Ajoutez votre premier produit avec son prix en FCFA et sa photo pour générer votre vitrine partageable.",
                        actionLabel = "Ajouter un produit",
                        onAction = {
                            editingProduct = null
                            showAddDialog = true
                        }
                    )
                }
            } else {
                items(filteredProducts, key = { it.id }) { prod ->
                    GlowCard(
                        accentColor = if (prod.available) EmeraldSecondary.copy(alpha = 0.5f) else CoralError.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Base64Thumbnail(
                                base64 = prod.imageBase64,
                                contentDescription = prod.name,
                                size = 72.dp,
                                fallbackIcon = Icons.Default.Inventory2
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    StatusPill(
                                        text = if (prod.available) "En stock" else "Épuisé",
                                        color = if (prod.available) EmeraldSecondary else CoralError
                                    )
                                }
                                Text(
                                    text = CopyGenerator.formatFcfa(prod.priceFcfa),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = prod.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (prod.description.isNotBlank()) {
                                    Text(
                                        text = prod.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                            Column {
                                IconButton(onClick = {
                                    editingProduct = prod
                                    showAddDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = GoldPrimary)
                                }
                                IconButton(onClick = { onDeleteProduct(prod.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = CoralError)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { name, price, cat, desc, imgB64, avail ->
                onSaveProduct(editingProduct?.id, name, price, cat, desc, imgB64, avail)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProductEditorDialog(
    initial: ProductItem?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, String, String?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf(initial?.priceFcfa?.toString() ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Général") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var available by remember { mutableStateOf(initial?.available ?: true) }
    var imageBase64 by remember { mutableStateOf(initial?.imageBase64) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = PosterRenderer.decodeUriToScaledBitmap(context, uri, maxDim = 640)
            if (bmp != null) {
                imageBase64 = PosterRenderer.bitmapToCompactBase64(bmp, maxDim = 480, quality = 65)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau produit" else "Modifier le produit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (imageBase64 == null) "Ajouter une photo" else "Photo sélectionnée (Changer)")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du produit *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_name")
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Prix (FCFA) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_price")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Produit disponible en stock")
                    Switch(checked = available, onCheckedChange = { available = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 0L
                    onSave(name, price, category, description, imageBase64, available)
                },
                enabled = !isBusy && name.isNotBlank(),
                modifier = Modifier.testTag("product_dialog_save")
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
