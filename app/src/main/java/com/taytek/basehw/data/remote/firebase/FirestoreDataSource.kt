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

    private fun userFoldersRef() = userId?.let {
        firestore.collection("users").document(it).collection("folders")
    }

    private fun userMappingsRef() = userId?.let {
        firestore.collection("users").document(it).collection("mappings")
    }

    private fun userPrefsDoc() = userId?.let {
        firestore.collection("users").document(it)
    }

    suspend fun saveUserPreferences(prefs: Map<String, Any>): Boolean {
        val doc = userPrefsDoc() ?: return false
        return try {
            doc.set(prefs, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchUserPreferences(): Map<String, Any>? {
        val doc = userPrefsDoc() ?: return null
        return try {
            val snapshot = doc.get().await()
            snapshot.data
        } catch (e: Exception) {
            null
        }
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

    suspend fun uploadFolderMap(data: Map<String, Any?>, existingFirestoreId: String = ""): String? {
        val ref = userFoldersRef() ?: return null
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

    suspend fun uploadMapping(folderFirestoreId: String, carFirestoreId: String): Boolean {
        val ref = userMappingsRef() ?: return false
        return try {
            // Document ID is a combination of both to avoid duplicates
            val mappingId = "${folderFirestoreId}_${carFirestoreId}"
            val data = mapOf(
                "folderId" to folderFirestoreId,
                "carId" to carFirestoreId
            )
            ref.document(mappingId).set(data).await()
            true
        } catch (e: Exception) {
            false
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

    suspend fun fetchAllFolders(): List<Map<String, Any>> {
        val ref = userFoldersRef() ?: return emptyList()
        return try {
            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.plus(mapOf("firestoreId" to doc.id))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchAllMappings(): List<Map<String, Any>> {
        val ref = userMappingsRef() ?: return emptyList()
        return try {
            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { it.data }
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

    suspend fun updateCarBackupPhotoUrl(firestoreId: String, photoUrl: String): Boolean {
        val ref = userCollectionRef() ?: return false
        return try {
            ref.document(firestoreId).update(
                mapOf(
                    "backupPhotoUrl" to photoUrl,
                    "userPhotoUrl" to photoUrl
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateAdditionalPhotosBackup(firestoreId: String, photoUrls: List<String>): Boolean {
        val ref = userCollectionRef() ?: return false
        return try {
            ref.document(firestoreId).update(
                mapOf(
                    "additionalPhotosBackup" to photoUrls
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearMappingsForFolder(folderFirestoreId: String): Boolean {
        val ref = userMappingsRef() ?: return false
        return try {
            val snapshot = ref.whereEqualTo("folderId", folderFirestoreId).get().await()
            snapshot.documents.forEach { it.reference.delete().await() }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFolder(firestoreId: String): Boolean {
        val ref = userFoldersRef() ?: return false
        return try {
            clearMappingsForFolder(firestoreId)
            ref.document(firestoreId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteUserAccountData(): Result<Unit> {
        val id = userId ?: return Result.failure(Exception("Kullanıcı bulunamadı."))
        return try {
            // Delete sub-collections first (Firebase doesn't delete sub-collections automatically)
            userCollectionRef()?.get()?.await()?.documents?.forEach { it.reference.delete().await() }
            userFoldersRef()?.get()?.await()?.documents?.forEach { it.reference.delete().await() }
            userMappingsRef()?.get()?.await()?.documents?.forEach { it.reference.delete().await() }
            
            // Delete the main user document
            firestore.collection("users").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
