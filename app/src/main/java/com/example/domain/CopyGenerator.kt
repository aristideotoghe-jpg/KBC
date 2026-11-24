package com.example.domain

import com.example.data.model.CopyStyle
import com.example.data.model.ProductItem
import com.example.data.model.SocialPlatform
import java.text.NumberFormat
import java.util.Locale

object CopyGenerator {

    fun formatFcfa(amount: Long): String {
        val formatter = NumberFormat.getIntegerInstance(Locale.FRANCE)
        return "${formatter.format(amount)} FCFA"
    }

    fun generateCommercialPost(
        businessName: String,
        productName: String,
        priceFcfa: Long,
        benefitOrDetails: String,
        contactPhone: String,
        platform: SocialPlatform,
        style: CopyStyle
    ): String {
        val shop = businessName.ifBlank { "Votre Boutique" }
        val priceText = formatFcfa(priceFcfa)
        val details = benefitOrDetails.ifBlank { "Qualité vérifiée et disponibilité immédiate." }
        val phoneLine = if (contactPhone.isNotBlank()) "Commandez vite au $contactPhone" else "Écrivez-nous en message privé pour commander"

        val body = when (style) {
            CopyStyle.DIRECT -> """
                🔥 NOUVEAUTÉ CHEZ $shop
                
                Découvrez : $productName
                💰 Prix : $priceText
                ✅ $details
                
                📲 $phoneLine
            """.trimIndent()

            CopyStyle.CHALEUREUX -> """
                Bonjour la famille ! ✨
                Chez $shop, nous avons pensé à vous avec un nouvel article coup de cœur : $productName.
                
                💛 Pourquoi vous allez l'adorer : $details
                💵 Tarif doux : $priceText
                
                Passez nous voir ou écrivez-nous : $phoneLine 😊
            """.trimIndent()

            CopyStyle.URGENCE -> """
                ⚡ OFFRE LIMITÉE — STOCK RAPIDE ⚡
                
                $productName disponible dès maintenant chez $shop !
                🏷️ Prix spécial : $priceText
                📌 $details
                
                ⏳ Premiers arrivés, premiers servis !
                👉 $phoneLine
            """.trimIndent()

            CopyStyle.PREMIUM -> """
                ✨ SÉLECTION EXCLUSIVE — $shop ✨
                
                Affirmez votre style avec $productName.
                $details
                
                💎 Investissement : $priceText
                📩 Réservation & livraison : $phoneLine
            """.trimIndent()
        }

        return when (platform) {
            SocialPlatform.WHATSAPP -> """
                *$productName* — *$priceText*
                
                ${body.replace("🔥 ", "").replace("✨ ", "")}
                
                _Livraison rapide ou retrait en boutique._
            """.trimIndent()

            SocialPlatform.FACEBOOK -> """
                $body
                
                👍 Aimez et partagez à un proche qui en a besoin !
                📍 Disponible chez $shop
            """.trimIndent()

            SocialPlatform.INSTAGRAM -> """
                $body
                
                .
                .
                #${shop.lowercase().replace(Regex("[^a-z0-9]"), "")} #commercegabon #libreville #bonplan #kombaboost #qualite
            """.trimIndent()
        }
    }

    fun generateStorefrontText(
        businessName: String,
        categoryLabel: String,
        phone: String?,
        products: List<ProductItem>
    ): String {
        val availableProducts = products.filter { it.available }
        val lines = if (availableProducts.isEmpty()) {
            "Catalogue en cours de mise à jour."
        } else {
            availableProducts.take(20).joinToString("\n") { p ->
                "• ${p.name} (${p.category}) — ${formatFcfa(p.priceFcfa)}"
            }
        }
        val contact = phone?.takeIf { it.isNotBlank() } ?: "Message privé"
        return """
            🛍️ *VITRINE OFFICIELLE — $businessName*
            📂 Activité : $categoryLabel
            
            $lines
            
            📲 Pour commander : $contact
            ✨ _Vitrine générée avec KOMBA BOOST_
        """.trimIndent()
    }

    fun generateLoyaltyOfferMessage(
        businessName: String,
        customerName: String,
        offerTitle: String,
        offerDetails: String
    ): String {
        return """
            Bonjour $customerName 👋
            Merci pour votre fidélité chez $businessName !
            
            🎁 *$offerTitle*
            $offerDetails
            
            Répondez à ce message pour en profiter dès aujourd'hui.
        """.trimIndent()
    }
}
