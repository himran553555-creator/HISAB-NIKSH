package com.example.ui.screens.auth

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.model.UserProfile
import com.example.util.PasswordUtil
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthViewModel"
private const val PREFS_NAME = "hisab_nikash_auth_prefs"
private const val KEY_REMEMBERED_IDENTIFIER = "remembered_identifier"
private const val KEY_REMEMBER_ME_ENABLED = "remember_me_enabled"

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: FirebaseUser) : AuthUiState
    data class Error(val message: String, val code: String? = null) : AuthUiState
}

sealed interface PasswordResetState {
    data object Idle : PasswordResetState
    data object Loading : PasswordResetState
    data class Success(val message: String) : PasswordResetState
    data class Error(val message: String) : PasswordResetState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun getRememberedIdentifier(): String {
        return prefs.getString(KEY_REMEMBERED_IDENTIFIER, "") ?: ""
    }

    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME_ENABLED, true)
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun resetPasswordResetState() {
        _passwordResetState.value = PasswordResetState.Idle
    }

    /**
     * Resolves raw identifier (email or phone) to Firebase-compatible email format.
     */
    fun resolveEmail(rawIdentifier: String, fallbackMobile: String = ""): String {
        val trimmed = rawIdentifier.trim().lowercase()
        return when {
            trimmed.contains("@") -> trimmed
            trimmed.isNotEmpty() -> {
                val sanitized = trimmed.replace(Regex("[^a-z0-9]"), "")
                "$sanitized@hisabnikash.com"
            }
            fallbackMobile.isNotEmpty() -> {
                val sanitized = fallbackMobile.replace(Regex("[^0-9]"), "")
                "$sanitized@hisabnikash.com"
            }
            else -> ""
        }
    }

    /**
     * Sign In with Email/Identifier & Password using Kotlin Coroutines
     */
    fun signIn(identifier: String, password: String, rememberMe: Boolean) {
        val trimmedId = identifier.trim()
        val trimmedPass = password.trim()

        if (trimmedId.isEmpty()) {
            _uiState.value = AuthUiState.Error("অনুগ্রহ করে ইমেইল বা মোবাইল নম্বর দিন (Please enter email or mobile number)")
            return
        }

        if (trimmedPass.isEmpty()) {
            _uiState.value = AuthUiState.Error("পাসওয়ার্ড প্রদান করুন (Please enter password)")
            return
        }

        val emailToUse = resolveEmail(trimmedId)

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(emailToUse, trimmedPass).await()
                val user = authResult.user

                // Persist Remember Me preferences for user session convenience
                prefs.edit().apply {
                    putBoolean(KEY_REMEMBER_ME_ENABLED, rememberMe)
                    if (rememberMe) {
                        putString(KEY_REMEMBERED_IDENTIFIER, trimmedId)
                    } else {
                        remove(KEY_REMEMBERED_IDENTIFIER)
                    }
                    apply()
                }

                if (user != null) {
                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("লগইন ব্যর্থ হয়েছে (Authentication failed)")
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                Log.e(TAG, "User not found", e)
                _uiState.value = AuthUiState.Error(
                    "এই ইমেইল বা নম্বরে কোনো অ্যাকাউন্ট পাওয়া যায়নি। অনুগ্রহ করে সাইন আপ করুন। (No account found with this identifier. Please sign up.)",
                    e.errorCode
                )
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "Invalid credentials", e)
                _uiState.value = AuthUiState.Error(
                    "ভুল পাসওয়ার্ড বা ইমেইল প্রদান করেছেন। অনুগ্রহ করে পুনরায় চেষ্টা করুন। (Incorrect email or password. Please verify or use Forgot Password.)",
                    e.errorCode
                )
            } catch (e: FirebaseNetworkException) {
                Log.e(TAG, "Network exception", e)
                _uiState.value = AuthUiState.Error(
                    "ইন্টারনেট সংযোগ ব্যর্থ হয়েছে। আপনার ডাটা বা ওয়াইফাই চেক করুন। (Network connection failed. Please check your internet connection.)"
                )
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "Firebase auth error: ${e.errorCode}", e)
                _uiState.value = AuthUiState.Error(
                    when (e.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> "এই অ্যাকাউন্টে তথ্য পাওয়া যায়নি (User not found)"
                        "ERROR_WRONG_PASSWORD" -> "ভুল পাসওয়ার্ড (Wrong password)"
                        "ERROR_INVALID_EMAIL" -> "সঠিক ইমেইল ঠিকানা দিন (Invalid email address)"
                        "ERROR_USER_DISABLED" -> "এই অ্যাকাউন্টটি নিষ্ক্রিয় করা হয়েছে (Account disabled)"
                        "ERROR_TOO_MANY_REQUESTS" -> "অতিরিক্ত প্রচেষ্টার কারণে সাময়িকভাবে ব্লক করা হয়েছে। কিছুক্ষণ পর চেষ্টা করুন। (Too many attempts. Try again later.)"
                        else -> e.localizedMessage ?: "লগইন ব্যর্থ হয়েছে (Login failed)"
                    },
                    e.errorCode
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected login failure", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "অপ্রত্যাশিত ত্রুটি ঘটেছে (Unexpected error occurred)")
            }
        }
    }

    /**
     * Sign Up with Email & Password using Kotlin Coroutines
     */
    fun signUp(
        fullName: String,
        businessName: String,
        mobile: String,
        rawEmail: String,
        password: String,
        confirmPassword: String,
        agreeToTerms: Boolean
    ) {
        val trimmedFullName = fullName.trim()
        val trimmedBizName = businessName.trim()
        val trimmedMobile = mobile.trim()
        val trimmedEmail = rawEmail.trim()
        val pass = password.trim()
        val confirmPass = confirmPassword.trim()

        if (!agreeToTerms) {
            _uiState.value = AuthUiState.Error("শর্তাবলী এবং গোপনীয়তা নীতিতে সম্মতি দিন (Please agree to Terms & Conditions)")
            return
        }

        val emailToUse = resolveEmail(trimmedEmail, trimmedMobile)
        if (emailToUse.isEmpty()) {
            _uiState.value = AuthUiState.Error("ইমেইল বা মোবাইল নম্বর দিন (Please provide an email or mobile number)")
            return
        }

        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে (Password must be at least 6 characters)")
            return
        }

        if (confirmPass.isNotEmpty() && pass != confirmPass) {
            _uiState.value = AuthUiState.Error("পাসওয়ার্ড দুটি মিলছে না (Passwords do not match)")
            return
        }

        _uiState.value = AuthUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val authResult = auth.createUserWithEmailAndPassword(emailToUse, pass).await()
                val user = authResult.user

                if (user != null) {
                    val finalName = trimmedFullName.ifEmpty { "Business Owner" }
                    val finalBiz = trimmedBizName.ifEmpty { "Hisab Nikash Business" }
                    val finalPhone = trimmedMobile.ifEmpty {
                        if (trimmedEmail.all { it.isDigit() }) trimmedEmail else "01700000000"
                    }

                    // Save User Profile in Firestore
                    val repository = FirestoreRepository(getApplication())
                    try {
                        repository.saveUserProfile(
                            UserProfile(
                                id = user.uid,
                                displayName = finalName,
                                businessName = finalBiz,
                                phone = finalPhone,
                                email = emailToUse,
                                currency = "BDT"
                            )
                        )
                        // Initialize business structures (E-SHOP, FRESHOO FOOD, PERSONAL ACCOUNT)
                        repository.initializeDefaultDataIfEmpty()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore profile save warning", e)
                    }

                    // Save identifier in prefs for future login convenience
                    prefs.edit().putString(KEY_REMEMBERED_IDENTIFIER, trimmedEmail.ifEmpty { trimmedMobile }).apply()

                    _uiState.value = AuthUiState.Success(user)
                } else {
                    _uiState.value = AuthUiState.Error("অ্যাকাউন্ট তৈরি ব্যর্থ হয়েছে (Failed to create account)")
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                Log.e(TAG, "Weak password error", e)
                _uiState.value = AuthUiState.Error(
                    "পাসওয়ার্ড অত্যন্ত দুর্বল। কমপক্ষে ৬টি অক্ষর, সংখ্যা ও প্রতীক ব্যবহার করুন। (Password is too weak. Please combine letters and numbers.)",
                    e.errorCode
                )
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.e(TAG, "User collision", e)
                _uiState.value = AuthUiState.Error(
                    "এই ইমেইল দিয়ে ইতিমধ্যে অ্যাকাউন্ট তৈরি করা আছে। লগইন ট্যাবে যান। (An account with this email/mobile already exists. Please switch to LOGIN.)",
                    e.errorCode
                )
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "Invalid email format", e)
                _uiState.value = AuthUiState.Error(
                    "সঠিক ইমেইল ফরম্যাট প্রদান করুন (Invalid email format)",
                    e.errorCode
                )
            } catch (e: FirebaseNetworkException) {
                Log.e(TAG, "Network error", e)
                _uiState.value = AuthUiState.Error("ইন্টারনেট সংযোগ চেক করুন (Please check your internet connection)")
            } catch (e: FirebaseAuthException) {
                Log.e(TAG, "Firebase auth error: ${e.errorCode}", e)
                _uiState.value = AuthUiState.Error(
                    e.localizedMessage ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে (Registration failed)",
                    e.errorCode
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign up failure", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "অপ্রত্যাশিত ত্রুটি ঘটেছে (Unexpected error)")
            }
        }
    }

    /**
     * Send Password Reset Email using Kotlin Coroutines
     */
    fun sendPasswordReset(rawEmail: String) {
        val trimmed = rawEmail.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            _passwordResetState.value = PasswordResetState.Error("সঠিক ইমেইল ঠিকানা প্রদান করুন (Please enter a valid email address)")
            return
        }

        _passwordResetState.value = PasswordResetState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                auth.sendPasswordResetEmail(trimmed).await()
                _passwordResetState.value = PasswordResetState.Success(
                    "পাসওয়ার্ড রিসেট লিংক আপনার ইমেইলে পাঠানো হয়েছে! ইনবক্স চেক করুন। (Password reset link sent! Check your inbox.)"
                )
            } catch (e: FirebaseAuthInvalidUserException) {
                _passwordResetState.value = PasswordResetState.Error("এই ইমেইলযুক্ত অ্যাকাউন্ট পাওয়া যায়নি (No account found with this email)")
            } catch (e: FirebaseNetworkException) {
                _passwordResetState.value = PasswordResetState.Error("ইন্টারনেট সংযোগ চেক করুন (Check internet connection)")
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetState.Error(e.localizedMessage ?: "রিসেট লিংক পাঠানো ব্যর্থ হয়েছে (Failed to send reset link)")
            }
        }
    }

    /**
     * Fast 1-Tap Instant Access using Kotlin Coroutines
     */
    fun performInstantAccess() {
        _uiState.value = AuthUiState.Loading
        val quickEmail = "owner.demo@hisabnikash.com"
        val quickPass = "HisabNikash2026!"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                try {
                    val authResult = auth.signInWithEmailAndPassword(quickEmail, quickPass).await()
                    val user = authResult.user
                    if (user != null) {
                        _uiState.value = AuthUiState.Success(user)
                        return@launch
                    }
                } catch (e: Exception) {
                    // Create if not exists
                    val createResult = auth.createUserWithEmailAndPassword(quickEmail, quickPass).await()
                    val newUser = createResult.user
                    if (newUser != null) {
                        val repo = FirestoreRepository(getApplication())
                        try {
                            repo.saveUserProfile(
                                UserProfile(
                                    id = newUser.uid,
                                    displayName = "Imran Business Owner",
                                    businessName = "Hisab Nikash Enterprise",
                                    phone = "01711002233",
                                    email = quickEmail,
                                    currency = "BDT"
                                )
                            )
                            repo.initializeDefaultDataIfEmpty()
                        } catch (ex: Exception) {
                            Log.w(TAG, "Demo profile init warning", ex)
                        }
                        _uiState.value = AuthUiState.Success(newUser)
                        return@launch
                    }
                }
                _uiState.value = AuthUiState.Error("তাৎক্ষণিক প্রবেশ ব্যর্থ হয়েছে (Instant access failed)")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "তাৎক্ষণিক প্রবেশ ব্যর্থ হয়েছে")
            }
        }
    }
}
