package com.taytek.basehw.data.remote.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FirebaseStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadUserCarPhoto(
        userId: String,
        localUriString: String
    ): String? {
        return try {
            android.util.Log.d("FirebaseStorage", "Starting upload for user: $userId, uri: $localUriString")
            val uri = Uri.parse(localUriString)
            val extension = "jpg" // default to jpg for safety
            val filename = "${UUID.randomUUID()}.$extension"
            
            val storageRef = storage.reference
                .child("users")
                .child(userId)
                .child("cars")
                .child(filename)

            val uploadTask = storageRef.putFile(uri).await()
            android.util.Log.d("FirebaseStorage", "Upload successful, fetching download URL...")
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            val finalUrl = downloadUrl.toString()
            android.util.Log.d("FirebaseStorage", "Download URL ready: $finalUrl")
            finalUrl
        } catch (e: Exception) {
            android.util.Log.e("FirebaseStorage", "Upload FAILED for $localUriString: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
