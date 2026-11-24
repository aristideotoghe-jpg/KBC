package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.MerchantProfile
import com.example.data.model.PaymentRequestItem
import com.example.data.model.SubscriptionPlanConfig
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import java.util.Date

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    suspend fun verifyServerAdminAccess(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.email == BOOTSTRAPPED_ADMIN_EMAIL && user.isEmailVerified) {
            return true
        }
        return try {
            val doc = db.collection("admins").document(user.uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeAllMerchants(adminUid: String): Flow<List<MerchantProfile>> {
        val path = "merchants"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MerchantProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == adminUid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeAllPayments(adminUid: String): Flow<List<PaymentRequestItem>> {
        val path = "payments"
        return db.collection(path)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PaymentRequestItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val ex = firestoreError ?: (error as? Exception)
                if (ex != null && auth.currentUser?.uid == adminUid) {
                    handleFirestoreError(ex, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun approvePaymentAndActivateSubscription(
        payment: PaymentRequestItem,
        adminNote: String
    ): Result<Unit> {
        val adminEmail = auth.currentUser?.email ?: return Result.failure(IllegalStateException("Admin non connecté"))
        val paymentRef = db.collection("payments").document(payment.id)
        val merchantRef = db.collection("merchants").document(payment.userId)
        return try {
            val renewalDate = Timestamp(Date(System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L))
            val batch = db.batch()
            batch.update(
                paymentRef,
                mapOf(
                    "status" to "approved",
                    "adminNote" to adminNote.trim().ifEmpty { "Paiement vérifié et validé" }.take(300),
                    "reviewedBy" to adminEmail.take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                merchantRef,
                mapOf(
                    "plan" to payment.planId,
                    "subscriptionStatus" to "active",
                    "renewalDate" to renewalDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, paymentRef.path)
            Result.failure(e)
        }
    }

    suspend fun rejectPayment(payment: PaymentRequestItem, reason: String): Result<Unit> {
        val adminEmail = auth.currentUser?.email ?: return Result.failure(IllegalStateException("Admin non connecté"))
        val paymentRef = db.collection("payments").document(payment.id)
        return try {
            paymentRef.update(
                mapOf(
                    "status" to "rejected",
                    "adminNote" to reason.trim().ifEmpty { "Référence introuvable ou montant incorrect" }.take(300),
                    "reviewedBy" to adminEmail.take(160),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, paymentRef.path)
            Result.failure(e)
        }
    }

    suspend fun setMerchantAccountStatus(merchantUserId: String, newStatus: String): Result<Unit> {
        val docRef = db.collection("merchants").document(merchantUserId)
        return try {
            require(newStatus in listOf("active", "suspended"))
            docRef.update(
                mapOf(
                    "accountStatus" to newStatus,
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

    suspend fun saveSubscriptionPlan(plan: SubscriptionPlanConfig): Result<Unit> {
        val docRef = db.collection("subscription_plans").document(plan.id)
        return try {
            val payload = mapOf(
                "name" to plan.name.trim().take(40),
                "priceFcfa" to plan.priceFcfa.coerceAtLeast(0L),
                "maxPostersPerMonth" to plan.maxPostersPerMonth.coerceAtLeast(1L),
                "maxProducts" to plan.maxProducts.coerceAtLeast(1L),
                "maxPostsPerMonth" to plan.maxPostsPerMonth.coerceAtLeast(1L),
                "maxCustomers" to plan.maxCustomers.coerceAtLeast(1L),
                "featuresSummary" to plan.featuresSummary.trim().take(400),
                "active" to plan.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "mitchdubois0789@gmail.com"
    }
}
