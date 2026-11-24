package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BusinessCategory(val id: String, val label: String) {
    VETEMENTS("vetements", "Vêtements & Mode"),
    RESTAURANT("restaurant", "Restaurant & Traiteur"),
    COSMETIQUES("cosmetiques", "Cosmétiques & Beauté"),
    CHAUSSURES("chaussures", "Chaussures & Maroquinerie"),
    DIVERS("divers", "Commerce Divers");

    companion object {
        fun fromId(raw: String): BusinessCategory =
            entries.firstOrNull { it.id == raw } ?: DIVERS
    }
}

enum class PlanTier(val id: String, val label: String, val defaultPriceFcfa: Long) {
    DECOUVERTE("decouverte", "DÉCOUVERTE", 0L),
    BUSINESS("business", "BUSINESS", 2000L),
    PREMIUM("premium", "PREMIUM", 5000L);

    companion object {
        fun fromId(raw: String): PlanTier =
            entries.firstOrNull { it.id == raw } ?: DECOUVERTE
    }
}

enum class PosterTemplate(
    val id: String,
    val label: String,
    val categoryHint: String
) {
    VETEMENTS_LUXE("vetements_luxe", "Atelier Mode Or", "Vêtements"),
    RESTO_SAVEURS("resto_saveurs", "Saveurs & Braise", "Restaurant"),
    COSMETIQUE_ECLAT("cosmetique_eclat", "Éclat Émeraude", "Cosmétiques"),
    CHAUSSURES_URBAN("chaussures_urban", "Street Néon", "Chaussures"),
    PROMO_FLASH("promo_flash", "Impact Promo Flash", "Commerces divers");

    companion object {
        fun fromId(raw: String): PosterTemplate =
            entries.firstOrNull { it.id == raw } ?: PROMO_FLASH
    }
}

enum class SocialPlatform(val id: String, val label: String) {
    WHATSAPP("whatsapp", "WhatsApp"),
    FACEBOOK("facebook", "Facebook"),
    INSTAGRAM("instagram", "Instagram");

    companion object {
        fun fromId(raw: String): SocialPlatform =
            entries.firstOrNull { it.id == raw } ?: WHATSAPP
    }
}

enum class CopyStyle(val id: String, val label: String) {
    DIRECT("direct", "Direct & Efficace"),
    CHALEUREUX("chaleureux", "Chaleureux & Proximité"),
    URGENCE("urgence", "Urgence Promo"),
    PREMIUM("premium", "Luxe & Prestige");

    companion object {
        fun fromId(raw: String): CopyStyle =
            entries.firstOrNull { it.id == raw } ?: DIRECT
    }
}

enum class PaymentProvider(val id: String, val label: String, val ussdHint: String) {
    AIRTEL_MONEY("airtel_money", "Airtel Money (Gabon)", "*150# — Envoi d'argent vers numéro marchand"),
    MOOV_MONEY("moov_money", "Moov Money / Mobile Money", "*555# — Transfert Mobile Money");

    companion object {
        fun fromId(raw: String): PaymentProvider =
            entries.firstOrNull { it.id == raw } ?: AIRTEL_MONEY
    }
}

@Keep
@IgnoreExtraProperties
data class MerchantProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val businessName: String = "",
    val businessCategory: String = BusinessCategory.DIVERS.id,
    val phone: String? = null,
    val plan: String = PlanTier.DECOUVERTE.id,
    val subscriptionStatus: String = "active",
    val accountStatus: String = "active",
    val renewalDate: Timestamp? = null,
    val creationsCount: Long = 0L,
    val postsCount: Long = 0L,
    val productsCount: Long = 0L,
    val sharesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val priceFcfa: Long = 0L,
    val category: String = "Général",
    val description: String = "",
    val imageBase64: String? = null,
    val available: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PosterItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val priceFcfa: Long = 0L,
    val description: String = "",
    val templateId: String = PosterTemplate.PROMO_FLASH.id,
    val promoBadge: String = "",
    val contactPhone: String = "",
    val imageBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPostItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val priceFcfa: Long = 0L,
    val platform: String = SocialPlatform.WHATSAPP.id,
    val style: String = CopyStyle.DIRECT.id,
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CalendarEventItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val platform: String = SocialPlatform.WHATSAPP.id,
    val campaignType: String = "Promotion",
    val scheduledDateEpochMs: Long = 0L,
    val notes: String = "",
    val status: String = "planned",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val consentGranted: Boolean = false,
    val notes: String = "",
    val lastOfferSent: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SubscriptionPlanConfig(
    @DocumentId val id: String = "",
    val name: String = "",
    val priceFcfa: Long = 0L,
    val maxPostersPerMonth: Long = 5L,
    val maxProducts: Long = 10L,
    val maxPostsPerMonth: Long = 10L,
    val maxCustomers: Long = 20L,
    val featuresSummary: String = "",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        fun defaults(): List<SubscriptionPlanConfig> = listOf(
            SubscriptionPlanConfig(
                id = PlanTier.DECOUVERTE.id,
                name = "DÉCOUVERTE",
                priceFcfa = 0L,
                maxPostersPerMonth = 5L,
                maxProducts = 10L,
                maxPostsPerMonth = 10L,
                maxCustomers = 15L,
                featuresSummary = "5 affiches/mois · 10 produits · Studio de textes · Vitrine WhatsApp",
                active = true
            ),
            SubscriptionPlanConfig(
                id = PlanTier.BUSINESS.id,
                name = "BUSINESS",
                priceFcfa = 2000L,
                maxPostersPerMonth = 40L,
                maxProducts = 60L,
                maxPostsPerMonth = 80L,
                maxCustomers = 150L,
                featuresSummary = "40 affiches/mois · 60 produits · Tous les modèles · Fidélisation 150 clients",
                active = true
            ),
            SubscriptionPlanConfig(
                id = PlanTier.PREMIUM.id,
                name = "PREMIUM",
                priceFcfa = 5000L,
                maxPostersPerMonth = 500L,
                maxProducts = 500L,
                maxPostsPerMonth = 1000L,
                maxCustomers = 2000L,
                featuresSummary = "Créations quasi illimitées · Catalogue complet · Campagnes & VIP illimités",
                active = true
            )
        )
    }
}

@Keep
@IgnoreExtraProperties
data class PaymentRequestItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val merchantName: String = "",
    val merchantEmail: String = "",
    val planId: String = PlanTier.BUSINESS.id,
    val amountFcfa: Long = 2000L,
    val provider: String = PaymentProvider.AIRTEL_MONEY.id,
    val senderPhone: String = "",
    val transactionRef: String = "",
    val proofNote: String = "",
    val proofImageBase64: String? = null,
    val status: String = "pending",
    val adminNote: String = "",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
