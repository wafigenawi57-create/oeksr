package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class AuthManager(private val context: Context) {
    private var auth: FirebaseAuth? = null
    var isFirebaseInitialized: Boolean = false
        private set

    init {
        try {
            // Check if Firebase is available and can be accessed
            FirebaseApp.getInstance()
            auth = FirebaseAuth.getInstance()
            isFirebaseInitialized = true
            Log.d("AuthManager", "Firebase Auth initialized successfully.")
        } catch (e: Exception) {
            isFirebaseInitialized = false
            Log.e("AuthManager", "Firebase is not configured or missing google-services.json: ${e.message}")
        }
    }

    // SharedPreferences for role persistency
    private val prefs = context.getSharedPreferences("oeks_auth_prefs", Context.MODE_PRIVATE)

    fun getUserRole(userIdOrEmail: String): String {
        return prefs.getString("role_$userIdOrEmail", null) ?: "Parent"
    }

    fun saveUserRole(userIdOrEmail: String, role: String) {
        prefs.edit().putString("role_$userIdOrEmail", role).apply()
    }

    fun getCurrentUser(): UserProfile? {
        val firebaseUser = auth?.currentUser
        if (firebaseUser != null) {
            val email = firebaseUser.email ?: ""
            val role = getUserRole(firebaseUser.uid)
            return UserProfile(email, role, isMock = false)
        }
        // Fallback/Check if sandbox user logged in
        val sandboxEmail = prefs.getString("sandbox_user_email", null)
        val sandboxRole = prefs.getString("sandbox_user_role", null)
        if (sandboxEmail != null && sandboxRole != null) {
            return UserProfile(sandboxEmail, sandboxRole, isMock = true)
        }
        return null
    }

    fun isFirebaseConfigured(): Boolean {
        return isFirebaseInitialized && auth != null
    }

    // Real Firebase Auth register
    fun registerWithFirebase(
        email: String,
        password: String,
        role: String,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            onFailure(Trans.t(
                "Firebase is not initialized. Please configure google-services.json or use Sandbox Bypass.",
                "قاعدة بيانات Firebase غير مفعّلة. يرجى تهيئة الملف أو تسجيل الدخول التجريبي."
            ))
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    saveUserRole(user.uid, role)
                    saveUserRole(email, role)
                    onSuccess(UserProfile(user.email ?: email, role, isMock = false))
                } else {
                    onFailure("Registration failed: User is null.")
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Registration failed.")
            }
    }

    // Real Firebase Auth sign in
    fun signInWithFirebase(
        email: String,
        password: String,
        onSuccess: (UserProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            onFailure(Trans.t(
                "Firebase is not initialized. Please configure google-services.json or use Sandbox Bypass.",
                "قاعدة بيانات Firebase غير مفعّلة. يرجى تهيئة الملف أو تسجيل الدخول التجريبي."
            ))
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val emailVal = user.email ?: email
                    val role = getUserRole(user.uid)
                    onSuccess(UserProfile(emailVal, role, isMock = false))
                } else {
                    onFailure("Sign in failed: User is null.")
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception.localizedMessage ?: "Sign in failed.")
            }
    }

    // Sandbox Mock Sign In (for preview / demo purposes)
    fun signInSandbox(email: String, role: String, onSuccess: (UserProfile) -> Unit) {
        prefs.edit().putString("sandbox_user_email", email).putString("sandbox_user_role", role).apply()
        saveUserRole(email, role)
        onSuccess(UserProfile(email, role, isMock = true))
    }

    fun signOut(onComplete: () -> Unit) {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthManager", "Error signing out: ${e.message}")
        }
        prefs.edit().remove("sandbox_user_email").remove("sandbox_user_role").apply()
        onComplete()
    }
}

data class UserProfile(
    val email: String,
    val role: String, // "Parent", "Teacher", "Manager"
    val isMock: Boolean = false
)
