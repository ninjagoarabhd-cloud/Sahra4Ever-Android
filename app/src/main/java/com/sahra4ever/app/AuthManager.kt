package com.sahra4ever.app

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

const val GUEST_NAME = "عضو سهرة فور إيفر"

data class AuthUser(val uid: String, val name: String, val photoUrl: String?)

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    name = displayName?.takeIf { it.isNotBlank() } ?: GUEST_NAME,
    photoUrl = photoUrl?.toString()
)

object AuthManager {
    private const val TAG = "AuthManager"
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _user = MutableStateFlow<AuthUser?>(null)
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    init {
        _user.value = auth.currentUser?.toAuthUser()
        auth.addAuthStateListener { _user.value = it.currentUser?.toAuthUser() }
    }

    suspend fun signInWithEmail(email: String, password: String) {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user?.let { publish(it) }
    }

    suspend fun registerWithEmail(name: String, email: String, password: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: throw IllegalStateException("No user returned")
        val update = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        user.updateProfile(update).await()
        publish(user)
    }

    suspend fun signOut() {
        auth.signOut()
    }

    private fun publish(user: FirebaseUser) {
        val u = user.toAuthUser()
        _user.value = u
        db.collection("users").document(u.uid)
            .set(
                mapOf(
                    "name" to u.name,
                    "photoUrl" to u.photoUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG, "Failed to save user profile", it) }
    }
}

fun friendlyAuthError(e: Exception): String? = when (e) {
    is FirebaseAuthWeakPasswordException -> "كلمة المرور ضعيفة (6 أحرف على الأقل)"
    is FirebaseAuthUserCollisionException -> "هذا البريد مسجّل مسبقاً، جرّب تسجيل الدخول"
    is FirebaseAuthInvalidUserException -> "لا يوجد حساب بهذا البريد"
    is FirebaseAuthInvalidCredentialsException -> "البريد أو كلمة المرور غير صحيحة"
    is FirebaseNetworkException -> "تحقق من اتصالك بالإنترنت"
    else -> "حدث خطأ، حاول مرة أخرى"
}