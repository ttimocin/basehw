package com.taytek.basehw.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.taytek.basehw.data.local.entity.UserCarEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore data source for GDPR-compliant user data sync.
 * Collection path: users/{uid}/collection/{carId}
 * Server location: europe-west3 (Frankfurt) — configured in Firebase Console.
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private fun userCollectionRef() = userId?.let {
        firestore.collection("users").document(it).collection("collection")
    }

    suspend fun uploadCar(car: UserCarEntity): String? {
        val ref = userCollectionRef() ?: return null
        return try {
            val data = mapOf(
                "masterDataId" to car.masterDataId,
                "isOpened" to car.isOpened,
                "purchaseDateMillis" to car.purchaseDateMillis,
                "personalNote" to car.personalNote,
                "storageLocation" to car.storageLocation,
                "isWishlist" to car.isWishlist,
                "localId" to car.id
            )
            val docRef = if (car.firestoreId.isBlank()) {
                ref.add(data).await()
            } else {
                ref.document(car.firestoreId).also { it.set(data).await() }
            }
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAllCars(): List<Map<String, Any>> {
        val ref = userCollectionRef() ?: return emptyList()
        return try {
            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.plus(mapOf("firestoreId" to doc.id))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteCar(firestoreId: String): Boolean {
        val ref = userCollectionRef() ?: return false
        return try {
            ref.document(firestoreId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
