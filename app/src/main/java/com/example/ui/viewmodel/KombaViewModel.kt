package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarEventItem
import com.example.data.model.CustomerItem
import com.example.data.model.MerchantProfile
import com.example.data.model.PaymentRequestItem
import com.example.data.model.PlanTier
import com.example.data.model.PosterItem
import com.example.data.model.ProductItem
import com.example.data.model.SocialPostItem
import com.example.data.model.SubscriptionPlanConfig
import com.example.data.repository.AdminRepository
import com.example.data.repository.KombaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class KombaViewModel(
    private val repo: KombaRepository,
    private val adminRepo: AdminRepository,
    private val currentUserId: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(tag: String, fallbackMsg: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(tag, fallbackMsg, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackMsg))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<MerchantProfile?>> =
        repo.observeMerchantProfile(currentUserId).toUiState("KombaVM", "Erreur chargement profil")

    val productsState: StateFlow<UiState<List<ProductItem>>> =
        repo.observeProducts(currentUserId).toUiState("KombaVM", "Erreur chargement catalogue")

    val postersState: StateFlow<UiState<List<PosterItem>>> =
        repo.observePosters(currentUserId).toUiState("KombaVM", "Erreur chargement affiches")

    val postsState: StateFlow<UiState<List<SocialPostItem>>> =
        repo.observePosts(currentUserId).toUiState("KombaVM", "Erreur chargement publications")

    val calendarState: StateFlow<UiState<List<CalendarEventItem>>> =
        repo.observeCalendarEvents(currentUserId).toUiState("KombaVM", "Erreur chargement calendrier")

    val customersState: StateFlow<UiState<List<CustomerItem>>> =
        repo.observeCustomers(currentUserId).toUiState("KombaVM", "Erreur chargement clients")

    val plansState: StateFlow<UiState<List<SubscriptionPlanConfig>>> =
        repo.observeSubscriptionPlans(currentUserId).toUiState("KombaVM", "Erreur chargement offres")

    val userPaymentsState: StateFlow<UiState<List<PaymentRequestItem>>> =
        repo.observeUserPayments(currentUserId).toUiState("KombaVM", "Erreur chargement paiements")

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val adminMerchantsState: StateFlow<UiState<List<MerchantProfile>>> =
        _isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else adminRepo.observeAllMerchants(currentUserId)
        }.toUiState("AdminVM", "Erreur chargement commerçants")

    val adminPaymentsState: StateFlow<UiState<List<PaymentRequestItem>>> =
        _isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else adminRepo.observeAllPayments(currentUserId)
        }.toUiState("AdminVM", "Erreur chargement paiements admin")

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            _isAdmin.value = adminRepo.verifyServerAdminAccess()
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun showMessage(msg: String) {
        _actionMessage.value = msg
    }

    fun activePlanConfig(): SubscriptionPlanConfig {
        val profile = (profileState.value as? UiState.Success)?.data
        val plans = (plansState.value as? UiState.Success)?.data ?: SubscriptionPlanConfig.defaults()
        val planId = profile?.plan ?: PlanTier.DECOUVERTE.id
        return plans.firstOrNull { it.id == planId } ?: SubscriptionPlanConfig.defaults().first()
    }

    // --- ONBOARDING & PROFILE ---

    fun createProfile(businessName: String, businessCategory: String, phone: String?) {
        runAction("Profil boutique créé avec succès !") {
            repo.createMerchantProfile(businessName, businessCategory, phone)
        }
    }

    fun updateBusinessInfo(businessName: String, businessCategory: String, phone: String?) {
        runAction("Informations mises à jour.") {
            repo.updateMerchantBusinessInfo(businessName, businessCategory, phone)
        }
    }

    fun recordShare() {
        viewModelScope.launch { repo.recordShareAction() }
    }

    // --- CATALOGUE ---

    fun saveProduct(
        productId: String?,
        name: String,
        priceFcfa: Long,
        category: String,
        description: String,
        imageBase64: String?,
        available: Boolean
    ) {
        if (productId == null) {
            val currentCount = (productsState.value as? UiState.Success)?.data?.size ?: 0
            val max = activePlanConfig().maxProducts
            if (currentCount >= max) {
                _actionMessage.value = "Limite atteinte ($max produits sur votre offre ${activePlanConfig().name}). Passez à une offre supérieure."
                return
            }
        }
        runAction(if (productId == null) "Produit ajouté au catalogue." else "Produit mis à jour.") {
            repo.saveProduct(productId, name, priceFcfa, category, description, imageBase64, available)
        }
    }

    fun deleteProduct(productId: String) {
        runAction("Produit supprimé.") { repo.deleteProduct(productId) }
    }

    // --- POSTERS ---

    fun savePoster(
        productName: String,
        priceFcfa: Long,
        description: String,
        templateId: String,
        promoBadge: String,
        contactPhone: String,
        imageBase64: String?
    ) {
        val currentCount = (postersState.value as? UiState.Success)?.data?.size ?: 0
        val max = activePlanConfig().maxPostersPerMonth
        if (currentCount >= max) {
            _actionMessage.value = "Limite atteinte ($max affiches sur votre offre ${activePlanConfig().name}). Passez à Business ou Premium."
            return
        }
        runAction("Affiche enregistrée dans votre Studio !") {
            repo.savePoster(productName, priceFcfa, description, templateId, promoBadge, contactPhone, imageBase64)
        }
    }

    // --- SOCIAL POSTS ---

    fun savePost(
        productName: String,
        priceFcfa: Long,
        platform: String,
        style: String,
        content: String
    ) {
        val currentCount = (postsState.value as? UiState.Success)?.data?.size ?: 0
        val max = activePlanConfig().maxPostsPerMonth
        if (currentCount >= max) {
            _actionMessage.value = "Limite atteinte ($max publications sur votre offre ${activePlanConfig().name})."
            return
        }
        runAction("Publication enregistrée.") {
            repo.savePost(productName, priceFcfa, platform, style, content)
        }
    }

    // --- CALENDAR ---

    fun addCalendarEvent(
        title: String,
        platform: String,
        campaignType: String,
        scheduledDateEpochMs: Long,
        notes: String
    ) {
        runAction("Campagne ajoutée au calendrier.") {
            repo.addCalendarEvent(title, platform, campaignType, scheduledDateEpochMs, notes)
        }
    }

    fun markCalendarEventStatus(eventId: String, status: String) {
        runAction("Statut mis à jour.") { repo.updateCalendarEventStatus(eventId, status) }
    }

    fun deleteCalendarEvent(eventId: String) {
        runAction("Événement supprimé.") { repo.deleteCalendarEvent(eventId) }
    }

    // --- LOYALTY (FIDÉLISATION) ---

    fun addCustomer(name: String, phone: String, consentGranted: Boolean, notes: String) {
        val currentCount = (customersState.value as? UiState.Success)?.data?.size ?: 0
        val max = activePlanConfig().maxCustomers
        if (currentCount >= max) {
            _actionMessage.value = "Limite atteinte ($max clients sur votre offre ${activePlanConfig().name})."
            return
        }
        runAction("Client enregistré avec consentement.") {
            repo.addCustomer(name, phone, consentGranted, notes)
        }
    }

    fun updateCustomerOffer(customerId: String, offerTitle: String) {
        viewModelScope.launch { repo.updateCustomerLastOffer(customerId, offerTitle) }
    }

    fun deleteCustomer(customerId: String) {
        runAction("Client retiré de la liste.") { repo.deleteCustomer(customerId) }
    }

    // --- PAYMENTS ---

    fun submitPayment(
        merchantName: String,
        merchantEmail: String,
        planId: String,
        amountFcfa: Long,
        provider: String,
        senderPhone: String,
        transactionRef: String,
        proofNote: String,
        proofImageBase64: String?
    ) {
        runAction("Preuve transmise ! Paiement en attente de vérification administrative.") {
            repo.submitMobileMoneyPayment(
                merchantName, merchantEmail, planId, amountFcfa,
                provider, senderPhone, transactionRef, proofNote, proofImageBase64
            )
        }
    }

    // --- ADMIN ACTIONS ---

    fun approvePayment(payment: PaymentRequestItem, adminNote: String) {
        if (!_isAdmin.value) return
        runAction("Paiement validé et abonnement activé !") {
            adminRepo.approvePaymentAndActivateSubscription(payment, adminNote)
        }
    }

    fun rejectPayment(payment: PaymentRequestItem, reason: String) {
        if (!_isAdmin.value) return
        runAction("Paiement refusé.") {
            adminRepo.rejectPayment(payment, reason)
        }
    }

    fun toggleMerchantSuspension(merchant: MerchantProfile) {
        if (!_isAdmin.value) return
        val next = if (merchant.accountStatus == "active") "suspended" else "active"
        val label = if (next == "suspended") "Compte suspendu." else "Compte réactivé."
        runAction(label) {
            adminRepo.setMerchantAccountStatus(merchant.userId, next)
        }
    }

    fun savePlanConfig(plan: SubscriptionPlanConfig) {
        if (!_isAdmin.value) return
        runAction("Offre ${plan.name} mise à jour.") {
            adminRepo.saveSubscriptionPlan(plan)
        }
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            _actionMessage.value = result.fold(
                onSuccess = { successMessage },
                onFailure = { it.localizedMessage ?: "Une erreur est survenue." }
            )
        }
    }
}
