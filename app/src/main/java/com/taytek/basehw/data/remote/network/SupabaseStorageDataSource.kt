package com.taytek.basehw.data.remote.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.taytek.basehw.data.remote.firebase.RemoteConfigDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: RemoteConfigDataSource,
    private val okHttpClient: OkHttpClient,
    private val firebaseAuth: FirebaseAuth
) {
    /** Firebase ID token al; başarısız olursa null döner (istek o zaman iptal edilir). */
    private suspend fun getFirebaseIdToken(): String? = runCatching {
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    }.onFailure {
        Log.w("SupabaseStorage", "Firebase token alınamadı", it)
    }.getOrNull()

    suspend fun uploadUserCarPhoto(userId: String, carId: Long, localUriString: String): String? {
        if (!remoteConfig.isPhotoBackupEnabled()) return null

        val baseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val apiKey = remoteConfig.getPhotoBackupApiKey().trim()
        val bucket = remoteConfig.getPhotoBackupBucket().trim()

        if (baseUrl.isBlank() || apiKey.isBlank() || bucket.isBlank()) return null

        val firebaseToken = getFirebaseIdToken()
        val authToken = firebaseToken ?: apiKey  // Firebase token varsa onu kullan, yoksa anon key

        val bytes = readImageBytes(localUriString) ?: return null
        val contentType = context.contentResolver.getType(Uri.parse(localUriString)) ?: "image/jpeg"
        val objectPath = "${userId}/cars/${carId}.jpg"

        Log.d(
            "SupabaseStorage",
            "Upload start: userId=$userId carId=$carId bucket=$bucket hasFirebaseToken=${firebaseToken != null} objectPath=$objectPath"
        )

        val uploadRequest = Request.Builder()
            .url("${baseUrl}/storage/v1/object/${bucket}/${objectPath}")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer ${authToken}")
            .header("x-upsert", "true")
            .put(bytes.toRequestBody(contentType.toMediaTypeOrNull()))
            .build()

        return runCatching {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(uploadRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string().orEmpty()
                        Log.e(
                            "SupabaseStorage",
                            "Upload failed: code=${response.code} hasFirebaseToken=${firebaseToken != null} objectPath=$objectPath body=$errorBody"
                        )
                        null
                    } else {
                        Log.d("SupabaseStorage", "Upload success: objectPath=$objectPath")
                        "${baseUrl}/storage/v1/object/public/${bucket}/${objectPath}"
                    }
                }
            }
        }.onFailure {
            Log.e("SupabaseStorage", "Photo backup upload error", it)
        }.getOrNull()
    }

    suspend fun deleteByPublicUrl(publicUrl: String): Boolean {
        if (!remoteConfig.isPhotoBackupEnabled()) return false

        val baseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val apiKey = remoteConfig.getPhotoBackupApiKey().trim()
        val bucket = remoteConfig.getPhotoBackupBucket().trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || bucket.isBlank()) return false

        val firebaseToken = getFirebaseIdToken()
        val authToken = firebaseToken ?: apiKey

        val marker = "/storage/v1/object/public/${bucket}/"
        val index = publicUrl.indexOf(marker)
        if (index < 0) return false

        val encodedPath = publicUrl.substring(index + marker.length).substringBefore("?")
        val objectPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name())

        val request = Request.Builder()
            .url("${baseUrl}/storage/v1/object/${bucket}/${objectPath}")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer ${authToken}")
            .delete()
            .build()

        return runCatching {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string().orEmpty()
                        Log.e("SupabaseStorage", "Delete failed: ${response.code} body=$errorBody")
                    }
                    response.isSuccessful
                }
            }
        }.onFailure {
            Log.e("SupabaseStorage", "Photo delete error", it)
        }.getOrDefault(false)
    }

    private fun readImageBytes(localUriString: String): ByteArray? {
        val uri = Uri.parse(localUriString)
        if (uri.scheme == "file") {
            return runCatching { File(requireNotNull(uri.path)).readBytes() }.getOrNull()
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }
}
