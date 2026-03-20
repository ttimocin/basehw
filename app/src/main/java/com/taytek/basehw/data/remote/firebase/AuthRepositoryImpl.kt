package com.taytek.basehw.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.taytek.basehw.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : AuthRepository {

    override val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<com.taytek.basehw.domain.model.User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // Fetch username if exists
                val username = fetchUsername(firebaseUser.uid)
                Result.success(com.taytek.basehw.domain.model.User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    username = username,
                    photoUrl = firebaseUser.photoUrl?.toString()
                ))
            } else {
                Result.failure(Exception("Kullanıcı bilgisi alınamadı."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<com.taytek.basehw.domain.model.User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val username = fetchUsername(firebaseUser.uid)
                Result.success(com.taytek.basehw.domain.model.User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    username = username,
                    photoUrl = firebaseUser.photoUrl?.toString()
                ))
            } else {
                Result.failure(Exception("Giriş başarısız."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, username: String): Result<com.taytek.basehw.domain.model.User> {
        return try {
            // 1. Check uniqueness again just in case
            val available = checkUsernameAvailable(username).getOrDefault(false)
            if (!available) return Result.failure(Exception("Bu kullanıcı adı zaten alınmış."))

            // 2. Create Auth user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Kayıt başarısız."))

            // 3. Save to Firestore
            saveUsername(firebaseUser.uid, username, email)

            Result.success(com.taytek.basehw.domain.model.User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                username = username,
                photoUrl = null
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(): Result<com.taytek.basehw.domain.model.User> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            val username = fetchUsername(user.uid)
            Result.success(com.taytek.basehw.domain.model.User(
                uid = user.uid,
                email = user.email ?: "",
                username = username,
                photoUrl = user.photoUrl?.toString()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection("usernames").document(username.lowercase()).get().await()
            Result.success(!snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUsername(username: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Oturum açılmamış."))
        return try {
            val oldUsername = fetchUsername(user.uid)
            saveUsername(user.uid, username, user.email ?: "", oldUsername)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchUsername(uid: String): String? {
        return try {
            firestore.collection("users").document(uid).get().await().getString("username")
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveUsername(uid: String, newUsername: String, email: String, oldUsername: String? = null) {
        val batch = firestore.batch()
        
        // Delete old username if it exists
        if (oldUsername != null && oldUsername.isNotBlank()) {
            val oldUsernameDoc = firestore.collection("usernames").document(oldUsername.lowercase())
            batch.delete(oldUsernameDoc)
        }
        
        // Lowercase for uniqueness check
        val usernameDoc = firestore.collection("usernames").document(newUsername.lowercase())
        batch.set(usernameDoc, mapOf(
            "uid" to uid,
            "original" to newUsername
        ))

        // User profile
        val userDoc = firestore.collection("users").document(uid)
        batch.set(userDoc, mapOf(
            "username" to newUsername,
            "email" to email
        ), com.google.firebase.firestore.SetOptions.merge()) // Use merge to not overwrite other fields
        
        batch.commit().await()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<com.taytek.basehw.domain.model.User> {
        return try {
            val result = auth.signInAnonymously().await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Result.success(com.taytek.basehw.domain.model.User(
                    uid = firebaseUser.uid,
                    email = "",
                    username = null
                ))
            } else {
                Result.failure(Exception("Anonim giriş başarısız."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı bulunamadı."))
            user.delete().await()
            auth.signOut() // Success deletion implicitly signs out, but explicit call ensures state update
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            // This is the most common reason for failure in sensitive operations
            Result.failure(Exception("REAUTH_REQUIRED"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendFeedback(feedback: com.taytek.basehw.domain.model.Feedback): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Kullanıcı bulunamadı."))
            val docRef = firestore.collection("feedback").document()
            val feedbackWithId = feedback.copy(id = docRef.id, userId = user.uid)
            docRef.set(feedbackWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
