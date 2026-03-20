package com.taytek.basehw.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<FirebaseUser?>
    val currentUser: FirebaseUser?
    
    suspend fun signInWithGoogle(idToken: String): Result<com.taytek.basehw.domain.model.User>
    suspend fun signInWithEmail(email: String, password: String): Result<com.taytek.basehw.domain.model.User>
    suspend fun signUpWithEmail(email: String, password: String, username: String): Result<com.taytek.basehw.domain.model.User>
    suspend fun getUserProfile(): Result<com.taytek.basehw.domain.model.User>
    suspend fun checkUsernameAvailable(username: String): Result<Boolean>
    suspend fun updateUsername(username: String): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signInAnonymously(): Result<com.taytek.basehw.domain.model.User>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun sendFeedback(feedback: com.taytek.basehw.domain.model.Feedback): Result<Unit>
}
