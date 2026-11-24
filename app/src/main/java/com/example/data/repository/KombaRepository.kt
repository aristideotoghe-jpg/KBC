package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CalendarEventItem
import com.example.data.model.CustomerItem
import com.example.data.model.MerchantProfile
import com.example.data.model.PaymentRequestItem
import com.example.data.model.PlanTier
import com.example.data.model.PosterItem
import com.example.data.model.ProductItem
import com.example.data.model.SocialPostItem
import com.example.data.model.SubscriptionPlanConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KombaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Connexion Google requise avant d'accéder aux données.")
    }

    private fun handleStreamError(error: Throwable, op: OperationType, path: String, uid: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    // --- MERCHANT PROFILE ---

    fun observeMerchantProfile(userId: String): Flow<MerchantProfile?> {
        require(userId.isNotBlank())
        val path = "merchants/$userId"
        return db.collection("merchants").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        MerchantProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error -> handleStreamError(error, OperationType.GET, path, userId) }
    }

    suspend fun createMerchantProfile(
        businessName: String,
        businessCategory: String,
        phone: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Non connecté"))
        val path = "merchants/$uid"
        return try {
            val user = auth.currentUser ?: error("Utilisateur non authentifié")
            val cleanName = businessName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Le nom de votre commerce est requis." }
            val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: cleanName).take(100)
            val email = (user.email?.takeIf { it.isNotBlank() } ?: "$uid@komba.local").take(160)
            val cleanPhone = phone?.trim()?.takeIf { it.isNotEmpty() }?.take(30)

            val payload = mapOf(
                "userId" to uid,
                "email" to email,
                "displayName" to displayName,
                "businessName" to cleanName,
                "businessCategory" to businessCategory,
                "phone" to cleanPhone,
                "plan" to PlanTier.DECOUVERTE.id,
                "subscriptionStatus" to "active",
                "accountStatus" to "active",
                "creationsCount" to 0L,
                "postsCount" to 0L,
                "productsCount" to 0L,
                "sharesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("merchants").document(uid).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMerchantBusinessInfo(
        businessName: String,
        businessCategory: String,
        phone: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Non connecté"))
        val path = "merchants/$uid"
        return try {
            val cleanName = businessName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Le nom du commerce est requis." }
            val cleanPhone = phone?.trim()?.take(30) ?: ""
            db.collection("merchants").document(uid).update(
                mapOf(
                    "businessName" to cleanName,
                    "businessCategory" to businessCategory,
                    "phone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordShareAction(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Non connecté"))
        val path = "merchants/$uid"
        return try {
            db.collection("merchants").document(uid).update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PRODUCTS (CATALOGUE) ---

    fun observeProducts(userId: String): Flow<List<ProductItem>> {
        require(userId.isNotBlank())
        val path = "merchants/$userId/products"
        return db.collection("merchants").document(userId).collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ProductItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    suspend fun saveProduct(
        productId: String?,
        name: String,
        priceFcfa: Long,
        category: String,
        description: String,
        imageBase64: String?,
        available: Boolean
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val col = db.collection("merchants").document(uid).collection("products")
        val docRef = if (productId.isNullOrBlank()) col.document() else col.document(productId)
        val isCreate = productId.isNullOrBlank()
        return try {
            val cleanName = name.trim().take(100)
            require(cleanName.isNotEmpty()) { "Le nom du produit est requis." }
            require(priceFcfa >= 0L) { "Le prix en FCFA doit être positif." }
            val cleanCategory = category.trim().ifEmpty { "Général" }.take(60)
            val cleanDesc = description.trim().take(600)
            val cleanImage = imageBase64?.takeIf { it.length <= 380_000 }

            if (isCreate) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "priceFcfa" to priceFcfa,
                    "category" to cleanCategory,
                    "description" to cleanDesc,
                    "imageBase64" to cleanImage,
                    "available" to available,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                val batch = db.batch()
                batch.set(docRef, payload)
                batch.update(
                    db.collection("merchants").document(uid),
                    mapOf(
                        "productsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "priceFcfa" to priceFcfa,
                    "category" to cleanCategory,
                    "description" to cleanDesc,
                    "available" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanImage != null) updates["imageBase64"] = cleanImage
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("products").document(productId)
        return try {
            require(productId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- POSTERS (STUDIO D'AFFICHES) ---

    fun observePosters(userId: String): Flow<List<PosterItem>> {
        require(userId.isNotBlank())
        val path = "merchants/$userId/posters"
        return db.collection("merchants").document(userId).collection("posters")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PosterItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    suspend fun savePoster(
        productName: String,
        priceFcfa: Long,
        description: String,
        templateId: String,
        promoBadge: String,
        contactPhone: String,
        imageBase64: String?
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("posters").document()
        return try {
            val cleanName = productName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Nom du produit requis pour l'affiche." }
            val cleanImage = imageBase64?.takeIf { it.length <= 380_000 }
            val payload = mapOf(
                "userId" to uid,
                "productName" to cleanName,
                "priceFcfa" to priceFcfa.coerceAtLeast(0L),
                "description" to description.trim().take(400),
                "templateId" to templateId,
                "promoBadge" to promoBadge.trim().take(30),
                "contactPhone" to contactPhone.trim().take(30),
                "imageBase64" to cleanImage,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            val batch = db.batch()
            batch.set(docRef, payload)
            batch.update(
                db.collection("merchants").document(uid),
                mapOf(
                    "creationsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SOCIAL POSTS (STUDIO DE PUBLICATIONS) ---

    fun observePosts(userId: String): Flow<List<SocialPostItem>> {
        require(userId.isNotBlank())
        val path = "merchants/$userId/posts"
        return db.collection("merchants").document(userId).collection("posts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SocialPostItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    suspend fun savePost(
        productName: String,
        priceFcfa: Long,
        platform: String,
        style: String,
        content: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("posts").document()
        return try {
            val cleanName = productName.trim().take(100)
            val cleanContent = content.trim().take(2500)
            require(cleanName.isNotEmpty() && cleanContent.isNotEmpty()) {
                "Nom du produit et texte de publication requis."
            }
            val payload = mapOf(
                "userId" to uid,
                "productName" to cleanName,
                "priceFcfa" to priceFcfa.coerceAtLeast(0L),
                "platform" to platform,
                "style" to style,
                "content" to cleanContent,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            batch.set(docRef, payload)
            batch.update(
                db.collection("merchants").document(uid),
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CALENDAR ---

    fun observeCalendarEvents(userId: String): Flow<List<CalendarEventItem>> {
        require(userId.isNotBlank())
        val path = "merchants/$userId/calendar"
        return db.collection("merchants").document(userId).collection("calendar")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CalendarEventItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.scheduledDateEpochMs }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    suspend fun addCalendarEvent(
        title: String,
        platform: String,
        campaignType: String,
        scheduledDateEpochMs: Long,
        notes: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("calendar").document()
        return try {
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Titre requis." }
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "platform" to platform,
                "campaignType" to campaignType.trim().ifEmpty { "Promotion" }.take(60),
                "scheduledDateEpochMs" to scheduledDateEpochMs,
                "notes" to notes.trim().take(500),
                "status" to "planned",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCalendarEventStatus(eventId: String, status: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("calendar").document(eventId)
        return try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("calendar").document(eventId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CUSTOMERS (FIDÉLISATION) ---

    fun observeCustomers(userId: String): Flow<List<CustomerItem>> {
        require(userId.isNotBlank())
        val path = "merchants/$userId/customers"
        return db.collection("merchants").document(userId).collection("customers")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomerItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    suspend fun addCustomer(
        name: String,
        phone: String,
        consentGranted: Boolean,
        notes: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("customers").document()
        return try {
            require(consentGranted) { "Le consentement du client est obligatoire." }
            val cleanName = name.trim().take(100)
            val cleanPhone = phone.trim().take(30)
            require(cleanName.isNotEmpty() && cleanPhone.length >= 4) {
                "Nom et numéro de téléphone valides requis."
            }
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "phone" to cleanPhone,
                "consentGranted" to true,
                "notes" to notes.trim().take(400),
                "lastOfferSent" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomerLastOffer(customerId: String, offerText: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("customers").document(customerId)
        return try {
            docRef.update(
                mapOf(
                    "lastOfferSent" to offerText.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("merchants").document(uid).collection("customers").document(customerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SUBSCRIPTION PLANS & PAYMENTS ---

    fun observeSubscriptionPlans(userId: String): Flow<List<SubscriptionPlanConfig>> {
        val path = "subscription_plans"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                val remote = snapshot.toObjects(
                    SubscriptionPlanConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.id }
                SubscriptionPlanConfig.defaults().map { def -> remote[def.id] ?: def }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    fun observeUserPayments(userId: String): Flow<List<PaymentRequestItem>> {
        require(userId.isNotBlank())
        val path = "payments"
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRequestItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error -> handleStreamError(error, OperationType.LIST, path, userId) }
    }

    suspend fun submitMobileMoneyPayment(
        merchantName: String,
        merchantEmail: String,
        planId: String,
        amountFcfa: Long,
        provider: String,
        senderPhone: String,
        transactionRef: String,
        proofNote: String,
        proofImageBase64: String?
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docRef = db.collection("payments").document()
        return try {
            val cleanPhone = senderPhone.trim().take(30)
            val cleanRef = transactionRef.trim().take(80)
            require(cleanPhone.length >= 6) { "Numéro Mobile Money invalide." }
            require(cleanRef.length >= 4) { "Référence de transaction invalide (min. 4 caractères)." }
            require(planId in listOf(PlanTier.BUSINESS.id, PlanTier.PREMIUM.id)) { "Offre invalide." }
            val cleanProof = proofImageBase64?.takeIf { it.length <= 380_000 }

            val payload = mapOf(
                "userId" to uid,
                "merchantName" to merchantName.trim().ifEmpty { "Commerçant" }.take(120),
                "merchantEmail" to merchantEmail.trim().ifEmpty { "$uid@komba.local" }.take(160),
                "planId" to planId,
                "amountFcfa" to amountFcfa.coerceAtLeast(1L),
                "provider" to provider,
                "senderPhone" to cleanPhone,
                "transactionRef" to cleanRef,
                "proofNote" to proofNote.trim().take(400),
                "proofImageBase64" to cleanProof,
                "status" to "pending",
                "adminNote" to "",
                "reviewedBy" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
