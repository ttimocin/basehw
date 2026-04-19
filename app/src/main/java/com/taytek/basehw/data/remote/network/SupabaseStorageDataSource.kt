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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
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
    private data class StorageEntry(
        val name: String,
        val id: String? = null
    )

    /** Firebase ID token al; başarısız olursa null döner (istek o zaman iptal edilir). */
    private suspend fun getFirebaseIdToken(): String? = runCatching {
        firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    }.onFailure {
        Log.w("SupabaseStorage", "Firebase token alınamadı", it)
    }.getOrNull()

    suspend fun uploadUserCarPhoto(userId: String, carId: Long, localUriString: String, suffix: String? = null): String? {
        if (!remoteConfig.isPhotoBackupEnabled()) return null

        val baseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val apiKey = remoteConfig.getPhotoBackupApiKey().trim()
        val bucket = remoteConfig.getPhotoBackupBucket().trim()

        if (baseUrl.isBlank() || apiKey.isBlank() || bucket.isBlank()) return null

        val firebaseToken = getFirebaseIdToken()
        // SECURITY: Firebase token zorunlu - anon key fallback kaldırıldı
        // Anon key ile istek atmak, herkesin dosya yükleyebilmesi anlamına gelir
        if (firebaseToken == null) {
            Log.w("SupabaseStorage", "Firebase token alınamadı, yükleme iptal edildi")
            return null
        }

        val bytes = readImageBytes(localUriString) ?: return null
        val contentType = context.contentResolver.getType(Uri.parse(localUriString)) ?: "image/jpeg"
        val objectPath = if (suffix != null) "${userId}/cars/${carId}_$suffix.jpg" else "${userId}/cars/${carId}.jpg"



        val uploadRequest = Request.Builder()
            .url("${baseUrl}/storage/v1/object/${bucket}/${objectPath}")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $firebaseToken")
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
                            "Upload failed: code=${response.code} objectPath=$objectPath body=$errorBody"
                        )
                        null
                    } else {

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
        // SECURITY: Firebase token zorunlu
        if (firebaseToken == null) {
            Log.w("SupabaseStorage", "Firebase token alınamadı, silme iptal edildi")
            return false
        }

        val marker = "/storage/v1/object/public/${bucket}/"
        val index = publicUrl.indexOf(marker)
        if (index < 0) return false

        val encodedPath = publicUrl.substring(index + marker.length).substringBefore("?")
        val objectPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name())

        val request = Request.Builder()
            .url("${baseUrl}/storage/v1/object/${bucket}/${objectPath}")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $firebaseToken")
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

    suspend fun deleteAllForUserPrefix(userId: String): Boolean {
        if (!remoteConfig.isPhotoBackupEnabled()) return false
        val prefix = "${userId}/"
        val allPaths = listAllObjectPaths(prefix)
        if (allPaths.isEmpty()) return true
        return deleteObjectPaths(allPaths)
    }

    suspend fun deleteByPublicUrls(publicUrls: Collection<String>): Boolean {
        if (!remoteConfig.isPhotoBackupEnabled()) return false
        val objectPaths = publicUrls
            .mapNotNull(::extractObjectPathFromPublicUrl)
            .distinct()
        if (objectPaths.isEmpty()) return true
        return deleteObjectPaths(objectPaths)
    }

    private suspend fun listAllObjectPaths(prefix: String): List<String> {
        val baseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val apiKey = remoteConfig.getPhotoBackupApiKey().trim()
        val bucket = remoteConfig.getPhotoBackupBucket().trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || bucket.isBlank()) return emptyList()

        val firebaseToken = getFirebaseIdToken() ?: return emptyList()

        val pendingPrefixes = ArrayDeque<String>().apply { add(prefix) }
        val collected = mutableListOf<String>()

        while (pendingPrefixes.isNotEmpty()) {
            val currentPrefix = pendingPrefixes.removeFirst()
            val requestBody = """{"prefix":"$currentPrefix","limit":1000,"offset":0}"""
                .toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${baseUrl}/storage/v1/object/list/${bucket}")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $firebaseToken")
                .post(requestBody)
                .build()

            val entries = runCatching {
                withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e(
                                "SupabaseStorage",
                                "List failed: code=${response.code} prefix=$currentPrefix body=${response.body?.string().orEmpty()}"
                            )
                            emptyList()
                        } else {
                            val payload = response.body?.string().orEmpty()
                            val array = JSONArray(payload)
                            buildList {
                                for (i in 0 until array.length()) {
                                    val item = array.getJSONObject(i)
                                    add(
                                        StorageEntry(
                                            name = item.optString("name"),
                                            id = item.optString("id").ifBlank { null }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }.getOrElse {
                Log.e("SupabaseStorage", "List objects error", it)
                emptyList()
            }

            entries.forEach { entry ->
                if (entry.name.isBlank()) return@forEach
                val fullPath = "$currentPrefix${entry.name}"
                if (entry.id == null) {
                    pendingPrefixes.add("$fullPath/")
                } else {
                    collected.add(fullPath)
                }
            }
        }
        return collected.distinct()
    }

    private suspend fun deleteObjectPaths(objectPaths: Collection<String>): Boolean {
        val baseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val apiKey = remoteConfig.getPhotoBackupApiKey().trim()
        val bucket = remoteConfig.getPhotoBackupBucket().trim()
        if (baseUrl.isBlank() || apiKey.isBlank() || bucket.isBlank()) return false

        val firebaseToken = getFirebaseIdToken() ?: return false
        val chunks = objectPaths.distinct().chunked(100)
        var allDeleted = true

        for (chunk in chunks) {
            // Supabase Storage REST API expects a JSON object, not a raw array (matches storage-js remove()).
            val body = JSONObject().apply {
                put("prefixes", JSONArray(chunk))
            }.toString()
            val request = Request.Builder()
                .url("${baseUrl}/storage/v1/object/${bucket}")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $firebaseToken")
                .delete(body.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val deleted = runCatching {
                withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e(
                                "SupabaseStorage",
                                "Bulk delete failed: code=${response.code} body=${response.body?.string().orEmpty()}"
                            )
                        }
                        response.isSuccessful
                    }
                }
            }.getOrElse {
                Log.e("SupabaseStorage", "Bulk delete error", it)
                false
            }
            if (!deleted) allDeleted = false
        }
        return allDeleted
    }

    private fun extractObjectPathFromPublicUrl(publicUrl: String): String? {
        val bucket = remoteConfig.getPhotoBackupBucket().trim()
        if (bucket.isBlank()) return null
        val marker = "/storage/v1/object/public/${bucket}/"
        val index = publicUrl.indexOf(marker)
        if (index < 0) return null
        val encodedPath = publicUrl.substring(index + marker.length).substringBefore("?")
        return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name())
    }

    private fun readImageBytes(localUriString: String): ByteArray? {
        val uri = Uri.parse(localUriString)
        if (uri.scheme == "file") {
            return runCatching { File(requireNotNull(uri.path)).readBytes() }.getOrNull()
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    /** Kullanıcı avatarını yükle */
    suspend fun uploadUserProfileAvatar(userId: String, localUriString: String): String? {
        if (!remoteConfig.isPhotoBackupEnabled()) return null

        val baseUrl = remoteConfig.getPhotoBackupSupabaseUrl().trim().trimEnd('/')
        val apiKey = remoteConfig.getPhotoBackupApiKey().trim()
        val bucket = remoteConfig.getPhotoBackupBucket().trim()

        if (baseUrl.isBlank() || apiKey.isBlank() || bucket.isBlank()) return null

        val firebaseToken = getFirebaseIdToken()
        // SECURITY: Firebase token zorunlu
        if (firebaseToken == null) {
            Log.w("SupabaseStorage", "Firebase token alınamadı, avatar yükleme iptal edildi")
            return null
        }

        val bytes = readImageBytes(localUriString) ?: return null
        val contentType = context.contentResolver.getType(Uri.parse(localUriString)) ?: "image/jpeg"
        val objectPath = "${userId}/avatar/profile_${UUID.randomUUID()}.jpg"



        val uploadRequest = Request.Builder()
            .url("${baseUrl}/storage/v1/object/${bucket}/${objectPath}")
            .header("apikey", apiKey)
            .header("Authorization", "Bearer $firebaseToken")
            .header("x-upsert", "true")
            .put(bytes.toRequestBody(contentType.toMediaTypeOrNull()))
            .build()

        return runCatching {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(uploadRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string().orEmpty()
                        Log.e("SupabaseStorage", "Avatar upload failed: code=${response.code} body=$errorBody")
                        null
                    } else {

                        "${baseUrl}/storage/v1/object/public/${bucket}/${objectPath}"
                    }
                }
            }
        }.onFailure {
            Log.e("SupabaseStorage", "Avatar upload error", it)
        }.getOrNull()
    }
}
