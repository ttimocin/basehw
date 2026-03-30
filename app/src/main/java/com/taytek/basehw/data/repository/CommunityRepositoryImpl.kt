package com.taytek.basehw.data.repository

import android.util.Log

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.taytek.basehw.domain.model.CommunityComment
import com.taytek.basehw.domain.model.CommunityPost
import com.taytek.basehw.domain.model.User
import com.taytek.basehw.domain.repository.CommunityRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val contentModerator: com.taytek.basehw.domain.util.ContentModerator
) : CommunityRepository {

    private val currentUid: String?
        get() = auth.currentUser?.uid

    // ── Posts ───────────────────────────────────────────────

    override suspend fun createPost(
        carModelName: String,
        carBrand: String,
        carYear: Int?,
        carSeries: String?,
        carImageUrl: String,
        caption: String,
        carFeature: String?
    ): Result<String> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        
        // AI Content Moderation (Anonymized: only caption is sent)
        val moderationResult = contentModerator.validateContent(caption).fold(
            onSuccess = { result -> result },
            onFailure = { e -> 
                Log.e("CommunityRepo", "Moderation API failure in createPost", e)
                return Result.failure(Exception("Topluluk kurallari denetlenemedi, lutfen tekrar deneyin."))
            }
        )

        if (!moderationResult.is_safe) {
            return Result.failure(Exception(moderationResult.reason ?: "Icerik kurallara aykiri bulundu."))
        }

        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "User"
            val authorIsAdmin = userDoc.getBoolean("isAdmin") ?: false
            
            val data = hashMapOf(
                "authorUid" to uid,
                "authorUsername" to username,
                "authorIsAdmin" to authorIsAdmin,
                "carModelName" to carModelName,
                "carBrand" to carBrand,
                "carYear" to carYear,
                "carSeries" to carSeries,
                "carImageUrl" to carImageUrl,
                "caption" to caption,
                "carFeature" to carFeature,
                "likeCount" to 0,
                "commentCount" to 0,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "isActive" to true
            )
            val docRef = firestore.collection("posts").add(data).await()

            // Increment user postCount
            firestore.collection("users").document(uid)
                .update("postCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFeedPosts(limit: Int): Result<List<CommunityPost>> {
        return try {
            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()

            Log.d("CommunityRepo", "getFeedPosts: ${snapshot.documents.size} docs found")

            val posts = snapshot.documents.mapNotNull { doc ->
                docToPost(doc)
            }.filter { it.isActive }

            // Check likes for current user
            val uid = currentUid
            if (uid != null) {
                val postsWithLikes = posts.map { post ->
                    val likeDoc = firestore.collection("posts").document(post.id)
                        .collection("likes").document(uid).get().await()
                    post.copy(isLikedByMe = likeDoc.exists())
                }
                Result.success(postsWithLikes)
            } else {
                Result.success(posts)
            }
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getFeedPosts error", e)
            Result.failure(e)
        }
    }

    override suspend fun getFollowingPosts(limit: Int): Result<List<CommunityPost>> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val followingUids = getFollowingUids().getOrDefault(emptyList())
            if (followingUids.isEmpty()) return Result.success(emptyList())

            // Firestore 'in' queries limited to 30 items
            val chunks = followingUids.chunked(30)
            val allPosts = mutableListOf<CommunityPost>()

            for (chunk in chunks) {
                val snapshot = firestore.collection("posts")
                    .whereIn("authorUid", chunk)
                    .get().await()

                snapshot.documents.mapNotNull { doc -> docToPost(doc) }
                    .filter { it.isActive }
                    .forEach { allPosts.add(it) }
            }

            // Sort combined and check likes
            val sorted = allPosts.sortedByDescending { it.createdAt }.take(limit)
            val postsWithLikes = sorted.map { post ->
                val likeDoc = firestore.collection("posts").document(post.id)
                    .collection("likes").document(uid).get().await()
                post.copy(isLikedByMe = likeDoc.exists())
            }
            Result.success(postsWithLikes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserPosts(uid: String, limit: Int): Result<List<CommunityPost>> {
        return try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("authorUid", uid)
                .get().await()

            val posts = snapshot.documents.mapNotNull { doc -> docToPost(doc) }
                .filter { it.isActive }
                .sortedByDescending { it.createdAt }
                .take(limit)

            val myUid = currentUid
            if (myUid != null) {
                val postsWithLikes = posts.map { post ->
                    val likeDoc = firestore.collection("posts").document(post.id)
                        .collection("likes").document(myUid).get().await()
                    post.copy(isLikedByMe = likeDoc.exists())
                }
                Result.success(postsWithLikes)
            } else {
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Likes ──────────────────────────────────────────────

    override suspend fun toggleLike(postId: String): Result<Boolean> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val likeRef = firestore.collection("posts").document(postId)
                .collection("likes").document(uid)
            val postRef = firestore.collection("posts").document(postId)

            val likeDoc = likeRef.get().await()
            if (likeDoc.exists()) {
                // Unlike
                val batch = firestore.batch()
                batch.delete(likeRef)
                batch.update(postRef, "likeCount", com.google.firebase.firestore.FieldValue.increment(-1))
                batch.commit().await()
                Result.success(false) // no longer liked
            } else {
                // Like
                val batch = firestore.batch()
                batch.set(likeRef, hashMapOf("timestamp" to com.google.firebase.Timestamp.now()))
                batch.update(postRef, "likeCount", com.google.firebase.firestore.FieldValue.increment(1))
                batch.commit().await()
                Result.success(true) // now liked
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Comments ───────────────────────────────────────────

    override suspend fun addComment(postId: String, text: String): Result<CommunityComment> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        
        // AI Content Moderation (Anonymized: only text is sent)
        val moderationResult = contentModerator.validateContent(text).fold(
            onSuccess = { result -> result },
            onFailure = { e -> 
                Log.e("CommunityRepo", "Moderation API failure in addComment", e)
                return Result.failure(Exception("Topluluk kuralları denetlenemedi, lütfen tekrar deneyin. (AI Bağlantı Hatası)"))
            }
        )

        if (!moderationResult.is_safe) {
            return Result.failure(Exception(moderationResult.reason ?: "İçerik kurallara aykırı bulundu."))
        }

        return try {
            val username = fetchUsername(uid) ?: "User"
            val now = System.currentTimeMillis()
            val data = hashMapOf(
                "authorUid" to uid,
                "authorUsername" to username,
                "text" to text,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            val docRef = firestore.collection("posts").document(postId)
                .collection("comments").add(data).await()

            // Increment commentCount
            firestore.collection("posts").document(postId)
                .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(CommunityComment(
                id = docRef.id,
                authorUid = uid,
                authorUsername = username,
                text = text,
                createdAt = now
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getComments(postId: String): Result<List<CommunityComment>> {
        return try {
            val snapshot = firestore.collection("posts").document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()

            val comments = snapshot.documents.mapNotNull { doc ->
                val timestamp = doc.getTimestamp("createdAt")
                CommunityComment(
                    id = doc.id,
                    authorUid = doc.getString("authorUid") ?: "",
                    authorUsername = doc.getString("authorUsername") ?: "",
                    text = doc.getString("text") ?: "",
                    createdAt = timestamp?.toDate()?.time ?: 0L
                )
            }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Follow ─────────────────────────────────────────────

    override suspend fun followUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        if (uid == targetUid) return Result.failure(Exception("Cannot follow yourself"))
        return try {
            val docId = "${uid}_${targetUid}"
            val data = hashMapOf(
                "followerUid" to uid,
                "followedUid" to targetUid,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            val batch = firestore.batch()
            batch.set(firestore.collection("follows").document(docId), data)
            batch.update(
                firestore.collection("users").document(uid),
                "followingCount", com.google.firebase.firestore.FieldValue.increment(1)
            )
            batch.update(
                firestore.collection("users").document(targetUid),
                "followerCount", com.google.firebase.firestore.FieldValue.increment(1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(targetUid: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val docId = "${uid}_${targetUid}"
            val batch = firestore.batch()
            batch.delete(firestore.collection("follows").document(docId))
            batch.update(
                firestore.collection("users").document(uid),
                "followingCount", com.google.firebase.firestore.FieldValue.increment(-1)
            )
            batch.update(
                firestore.collection("users").document(targetUid),
                "followerCount", com.google.firebase.firestore.FieldValue.increment(-1)
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFollowing(targetUid: String): Result<Boolean> {
        val uid = currentUid ?: return Result.success(false)
        return try {
            val docId = "${uid}_${targetUid}"
            val doc = firestore.collection("follows").document(docId).get().await()
            Result.success(doc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFollowingUids(): Result<List<String>> {
        val uid = currentUid ?: return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection("follows")
                .whereEqualTo("followerUid", uid)
                .get().await()
            val uids = snapshot.documents.mapNotNull { it.getString("followedUid") }
            Result.success(uids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── User Profile ───────────────────────────────────────

    override suspend fun getUserProfile(uid: String): Result<User> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val user = User(
                uid = uid,
                email = doc.getString("email") ?: "",
                username = doc.getString("username"),
                photoUrl = doc.getString("photoUrl"),
                followerCount = doc.getLong("followerCount")?.toInt() ?: 0,
                followingCount = doc.getLong("followingCount")?.toInt() ?: 0,
                postCount = doc.getLong("postCount")?.toInt() ?: 0,
                rulesAccepted = doc.getBoolean("rulesAccepted") ?: false,
                isAdmin = doc.getBoolean("isAdmin") ?: false
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopUsers(limit: Int): Result<List<User>> {
        return try {
            val snapshot = firestore.collection("users")
                .orderBy("postCount", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get().await()

            val users = snapshot.documents.map { doc ->
                User(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username"),
                    photoUrl = doc.getString("photoUrl"),
                    followerCount = doc.getLong("followerCount")?.toInt() ?: 0,
                    followingCount = doc.getLong("followingCount")?.toInt() ?: 0,
                    postCount = doc.getLong("postCount")?.toInt() ?: 0,
                    rulesAccepted = doc.getBoolean("rulesAccepted") ?: false
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "getTopUsers error", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            val postRef = firestore.collection("posts").document(postId)
            val postDoc = postRef.get().await()
            
            if (!postDoc.exists()) return Result.failure(Exception("Post not found"))
            
            val authorUid = postDoc.getString("authorUid")
            if (authorUid != uid) {
                return Result.failure(Exception("You can only delete your own posts"))
            }

            val userRef = firestore.collection("users").document(uid)

            firestore.runBatch { batch ->
                batch.delete(postRef)
                batch.update(userRef, "postCount", com.google.firebase.firestore.FieldValue.increment(-1))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CommunityRepo", "deletePost error", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            Log.d("CommunityRepo", "Deleting comment: $commentId from post: $postId")
            val postRef = firestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document(commentId)

            firestore.runBatch { batch ->
                batch.delete(commentRef)
                batch.update(postRef, "commentCount", com.google.firebase.firestore.FieldValue.increment(-1))
            }.await()
            
            Log.d("CommunityRepo", "Comment deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptRules(): Result<Unit> {
        val uid = currentUid ?: return Result.failure(Exception("Not signed in"))
        return try {
            firestore.collection("users").document(uid).update("rulesAccepted", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers ─────────────────────────────────────────────

    private suspend fun fetchUsername(uid: String): String? {
        return try {
            firestore.collection("users").document(uid).get().await().getString("username")
        } catch (e: Exception) {
            null
        }
    }

    private fun docToPost(doc: com.google.firebase.firestore.DocumentSnapshot): CommunityPost? {
        return try {
            val timestamp = doc.getTimestamp("createdAt")
            CommunityPost(
                id = doc.id,
                authorUid = doc.getString("authorUid") ?: return null,
                authorUsername = doc.getString("authorUsername") ?: "",
                carModelName = doc.getString("carModelName") ?: "",
                carBrand = doc.getString("carBrand") ?: "",
                carYear = doc.getLong("carYear")?.toInt(),
                carSeries = doc.getString("carSeries"),
                carImageUrl = doc.getString("carImageUrl") ?: "",
                caption = doc.getString("caption") ?: "",
                carFeature = doc.getString("carFeature"),
                authorIsAdmin = doc.getBoolean("authorIsAdmin") ?: false,
                likeCount = doc.getLong("likeCount")?.toInt() ?: 0,
                commentCount = doc.getLong("commentCount")?.toInt() ?: 0,
                createdAt = timestamp?.toDate()?.time ?: 0L,
                isActive = doc.getBoolean("isActive") ?: true
            )
        } catch (e: Exception) {
            null
        }
    }
}
