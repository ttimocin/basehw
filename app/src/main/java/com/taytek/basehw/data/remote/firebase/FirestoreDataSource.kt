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

    suspend fun uploadCarMap(data: Map<String, Any?>, existingFirestoreId: String = ""): String? {
        val ref = userCollectionRef() ?: return null
        return try {
            val docRef = if (existingFirestoreId.isBlank()) {
                ref.add(data).await()
            } else {
                ref.document(existingFirestoreId).also { it.set(data).await() }
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
