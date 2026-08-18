package com.example.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val context: Context) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    suspend fun signInWithGoogle(webClientId: String = ""): Result<FirebaseUser> {
        return try {
            val serverClientId = webClientId.ifBlank {
                "544558352297-mock.apps.googleusercontent.com"
            }

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to obtain Firebase user"))
                }
            } else {
                Result.failure(Exception("Unsupported credential type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val res = auth.signInWithEmailAndPassword(email, pass).await()
            val user = res.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmailPassword(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = res.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign up failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
