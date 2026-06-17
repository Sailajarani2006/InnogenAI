package com.innogen.aipro.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.innogen.aipro.domain.model.User
import com.innogen.aipro.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth     : FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: return Result.failure(Exception("Sign in failed"))
            val user   = fbUser.toUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: return Result.failure(Exception("Sign up failed"))

            // Update display name
            val profileUpdates = userProfileChangeRequest { displayName = name }
            fbUser.updateProfile(profileUpdates).await()

            // Save user to Firestore
            val userData = mapOf(
                "uid"      to fbUser.uid,
                "email"    to email,
                "name"     to name,
                "plan"     to "free",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(fbUser.uid).set(userData).await()

            Result.success(User(uid = fbUser.uid, email = email, name = name))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result     = auth.signInWithCredential(credential).await()
            val fbUser     = result.user ?: return Result.failure(Exception("Google sign in failed"))

            // Upsert user in Firestore
            val userData = mapOf(
                "uid"      to fbUser.uid,
                "email"    to (fbUser.email ?: ""),
                "name"     to (fbUser.displayName ?: ""),
                "photoUrl" to (fbUser.photoUrl?.toString() ?: ""),
                "plan"     to "free"
            )
            firestore.collection("users").document(fbUser.uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(fbUser.toUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun getCurrentUser(): User? = auth.currentUser?.toUser()

    override val isLoggedIn: Boolean get() = auth.currentUser != null

    private fun com.google.firebase.auth.FirebaseUser.toUser() = User(
        uid      = uid,
        email    = email ?: "",
        name     = displayName ?: "",
        photoUrl = photoUrl?.toString() ?: ""
    )
}
