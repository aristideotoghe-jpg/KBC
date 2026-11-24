package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessCategory
import com.example.ui.components.GlowCard
import com.example.ui.components.KombaLogoHeader
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.ObsidianBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    defaultDisplayName: String,
    isBusy: Boolean,
    onSubmit: (businessName: String, categoryId: String, phone: String?) -> Unit,
    onSignOut: () -> Unit
) {
    var businessName by remember { mutableStateOf(defaultDisplayName.ifBlank { "" }) }
    var selectedCategory by remember { mutableStateOf(BusinessCategory.VETEMENTS) }
    var phone by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                KombaLogoHeader(subtitle = "Configuration rapide (1 minute)")
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Se déconnecter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            GlowCard(accentColor = GoldPrimary) {
                Text(
                    text = "Bienvenue dans votre espace commerçant",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Choisissez votre activité commerciale pour personnaliser vos modèles d'affiches et vos messages publicitaires.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Nom de votre commerce / boutique *") },
                placeholder = { Text("Ex : Boutique Élégance Libreville") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_business_name")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Votre secteur d'activité *",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BusinessCategory.entries.forEach { cat ->
                        val selected = cat == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary.copy(alpha = 0.22f),
                                selectedLabelColor = GoldPrimary,
                                selectedLeadingIconColor = GoldPrimary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) GoldPrimary else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Numéro WhatsApp / Contact client (optionnel)") },
                placeholder = { Text("Ex : +241 074 00 00 00") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_phone")
            )

            Button(
                onClick = {
                    onSubmit(businessName, selectedCategory.id, phone.ifBlank { null })
                },
                enabled = !isBusy && businessName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_submit_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Ouvrir mon Tableau de bord", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
