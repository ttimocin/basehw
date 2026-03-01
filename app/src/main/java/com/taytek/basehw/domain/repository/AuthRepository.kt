package com.taytek.basehw.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<FirebaseUser?>
    val currentUser: FirebaseUser?
    
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun signInAnonymously(): Result<FirebaseUser>
    suspend fun signOut(): Result<Unit>
}
