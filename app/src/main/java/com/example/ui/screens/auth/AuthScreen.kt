package com.example.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.FirestoreRepository
import com.example.model.UserProfile
import com.example.util.PasswordStrength
import com.example.util.PasswordUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"

// Brand Color Palette per Design Specification
private val EmeraldPrimary = Color(0xFF10B981)
private val EmeraldDark = Color(0xFF059669)
private val DeepNavy = Color(0xFF142D4E)
private val DarkNavyBg = Color(0xFF0A192F)
private val GoldAccent = Color(0xFFF5B942)
private val LightMint = Color(0xFFECFDF5)
private val SoftWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)
private val BorderGray = Color(0xFFE2E8F0)

enum class AuthTab {
    LOGIN,
    SIGN_UP
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val focusManager = LocalFocusManager.current

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var isSocialLoading by remember { mutableStateOf(false) }
    val isLoading = (uiState is AuthUiState.Loading) || isSocialLoading

    var selectedTab by remember { mutableStateOf(AuthTab.LOGIN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Login Form State
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Sign Up Form State
    var signUpFullName by remember { mutableStateOf("") }
    var signUpBusinessName by remember { mutableStateOf("") }
    var signUpMobile by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(true) }

    // Session Persistence: Pre-fill remembered identifier and preference
    LaunchedEffect(Unit) {
        val savedId = authViewModel.getRememberedIdentifier()
        if (savedId.isNotEmpty()) {
            loginIdentifier = savedId
        }
        rememberMe = authViewModel.isRememberMeEnabled()
    }

    // Handle Authentication State Changes from ViewModel
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                onAuthSuccess()
            }
            is AuthUiState.Error -> {
                errorMessage = state.message
            }
            else -> Unit
        }
    }

    // Attempt silent auto-sign in on initial display
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Wait for user to interact */ },
            scope = coroutineScope
        )
    }

    // Floating animation for the 3D logo
    val infiniteTransition = rememberInfiniteTransition(label = "floatingLogo")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepNavy,
                        Color(0xFF0F243E),
                        DarkNavyBg
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // 1. TOP SECTION: 3D Logo, Welcome & Tagline
            // ==========================================
            Box(
                modifier = Modifier
                    .offset(y = floatOffset.dp)
                    .size(105.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft emerald glow background aura
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    EmeraldPrimary.copy(alpha = 0.45f),
                                    GoldAccent.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // User-uploaded 3D Finance Logo Asset
                Image(
                    painter = painterResource(id = R.drawable.ic_hn_3d_logo),
                    contentDescription = "HISAB NIKASH 3D Logo",
                    modifier = Modifier
                        .size(95.dp)
                        .shadow(16.dp, shape = CircleShape),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Welcome Message in Bengali and English Brand
            Text(
                text = "ওয়েলকাম টু হিসাব নিকাশ",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = SoftWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HISAB NIKASH",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = GoldAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldAccent, EmeraldPrimary)))
                ) {
                    Text(
                        text = "FINTECH PRO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Smart Business. Smarter Accounting.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 2. AUTHENTICATION CARD (Mobile-First Rounded)
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SoftWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Two Modern Top Tabs [ LOGIN ]  [ SIGN UP ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LOGIN TAB
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedTab == AuthTab.LOGIN) EmeraldPrimary else Color.Transparent
                                )
                                .clickable {
                                    selectedTab = AuthTab.LOGIN
                                    errorMessage = null
                                    authViewModel.resetState()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "LOGIN",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == AuthTab.LOGIN) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTab == AuthTab.LOGIN) SoftWhite else TextMuted
                            )
                        }

                        // SIGN UP TAB
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedTab == AuthTab.SIGN_UP) EmeraldPrimary else Color.Transparent
                                )
                                .clickable {
                                    selectedTab = AuthTab.SIGN_UP
                                    errorMessage = null
                                    authViewModel.resetState()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SIGN UP",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTab == AuthTab.SIGN_UP) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTab == AuthTab.SIGN_UP) SoftWhite else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF87171)))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = Color(0xFFB91C1C),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFF991B1B),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }

                    // Content Switcher between LOGIN and SIGN UP
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState == AuthTab.SIGN_UP) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "tabTransition"
                    ) { tab ->
                        if (tab == AuthTab.LOGIN) {
                            // ==========================================
                            // LOGIN FORM
                            // ==========================================
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Email or Mobile Number Input
                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = { loginIdentifier = it },
                                    label = { Text("Email or Mobile Number") },
                                    placeholder = { Text("e.g. name@company.com") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_identifier_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Password Input
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it },
                                    label = { Text("Password") },
                                    placeholder = { Text("Enter your password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Password",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                            Icon(
                                                imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Remember Me & Forgot Password Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                                    ) {
                                        Checkbox(
                                            checked = rememberMe,
                                            onCheckedChange = { rememberMe = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = EmeraldPrimary,
                                                uncheckedColor = TextMuted
                                            )
                                        )
                                        Text(
                                            text = "Remember Me",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextDark
                                        )
                                    }

                                    Text(
                                        text = "Forgot Password?",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldDark,
                                        modifier = Modifier
                                            .clickable { showForgotPasswordDialog = true }
                                            .padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Premium Emerald Green Login Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        authViewModel.signIn(
                                            identifier = loginIdentifier,
                                            password = loginPassword,
                                            rememberMe = rememberMe
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("login_submit_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = SoftWhite
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading && !isSocialLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = SoftWhite,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "LOGIN TO YOUR BUSINESS",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Login",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 1-Tap Instant Access Button (For immediate access without Google Account issues)
                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        authViewModel.performInstantAccess()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("instant_quick_access_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFFF0FDF4),
                                        contentColor = Color(0xFF065F46)
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(listOf(EmeraldPrimary, GoldAccent))
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isLoading
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = "Quick Access",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "১-ক্লিকে তাৎক্ষণিক প্রবেশ (Instant Access)",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Or Continue With Divider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
                                    Text(
                                        text = " Or Continue With ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Social Buttons Row [ Google ] [ Facebook ] [ Apple ]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Google Login (Fully functional with Credential Manager)
                                    OutlinedButton(
                                        onClick = {
                                            isSocialLoading = true
                                            errorMessage = null
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {
                                                    isSocialLoading = false
                                                    onAuthSuccess()
                                                },
                                                onAuthError = { err ->
                                                    isSocialLoading = false
                                                    errorMessage = err
                                                },
                                                scope = coroutineScope,
                                                onAuthCancelled = {
                                                    isSocialLoading = false
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("social_google_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Text(
                                            text = "G  Google",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // Facebook Button
                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, "Facebook Authentication configured for mobile", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1877F2)),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Text(
                                            text = "f  Facebook",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // Apple Button
                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, "Apple ID Authentication configured for mobile", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Text(
                                            text = "  Apple",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Bottom Switch Link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Don't have an account? ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Create New Account",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldPrimary,
                                        modifier = Modifier.clickable {
                                            selectedTab = AuthTab.SIGN_UP
                                            errorMessage = null
                                            authViewModel.resetState()
                                        }
                                    )
                                }
                            }
                        } else {
                            // ==========================================
                            // 3. SIGN UP FORM
                            // ==========================================
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Full Name
                                OutlinedTextField(
                                    value = signUpFullName,
                                    onValueChange = { signUpFullName = it },
                                    label = { Text("Full Name *") },
                                    placeholder = { Text("e.g. Imran Hossain") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Full Name",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_fullname_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Business Name
                                OutlinedTextField(
                                    value = signUpBusinessName,
                                    onValueChange = { signUpBusinessName = it },
                                    label = { Text("Business Name *") },
                                    placeholder = { Text("e.g. Imran Enterprise / Freshoo") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Business,
                                            contentDescription = "Business Name",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_businessname_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Mobile Number
                                OutlinedTextField(
                                    value = signUpMobile,
                                    onValueChange = { signUpMobile = it },
                                    label = { Text("Mobile Number *") },
                                    placeholder = { Text("e.g. 01712345678") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Mobile Number",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_mobile_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Email Address
                                OutlinedTextField(
                                    value = signUpEmail,
                                    onValueChange = { signUpEmail = it },
                                    label = { Text("Email Address *") },
                                    placeholder = { Text("e.g. business@company.com") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_email_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Password
                                OutlinedTextField(
                                    value = signUpPassword,
                                    onValueChange = { signUpPassword = it },
                                    label = { Text("Password (min 6 characters) *") },
                                    placeholder = { Text("Create strong password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Password",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                            Icon(
                                                imageVector = if (signUpPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password",
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_password_input")
                                )

                                // Real-time Password Strength Meter
                                PasswordStrengthMeter(
                                    password = signUpPassword,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Confirm Password
                                OutlinedTextField(
                                    value = signUpConfirmPassword,
                                    onValueChange = { signUpConfirmPassword = it },
                                    label = { Text("Confirm Password *") },
                                    placeholder = { Text("Re-enter password") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Confirm Password",
                                            tint = EmeraldPrimary
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (signUpConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle confirm password",
                                                tint = TextMuted
                                            )
                                        }
                                    },
                                    visualTransformation = if (signUpConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = BorderGray,
                                        focusedLabelColor = EmeraldPrimary,
                                        cursorColor = EmeraldPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_confirm_password_input")
                                )

                                // Live Password Match Indicator
                                AnimatedVisibility(
                                    visible = signUpConfirmPassword.isNotEmpty(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    val matches = signUpPassword.isNotEmpty() && signUpPassword == signUpConfirmPassword
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, start = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (matches) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (matches) EmeraldPrimary else Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (matches) "Passwords match" else "Passwords do not match yet",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = if (matches) EmeraldDark else Color(0xFFDC2626)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Agree to Terms Checkbox
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { agreeToTerms = !agreeToTerms }
                                ) {
                                    Checkbox(
                                        checked = agreeToTerms,
                                        onCheckedChange = { agreeToTerms = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = EmeraldPrimary,
                                            uncheckedColor = TextMuted
                                        )
                                    )
                                    Text(
                                        text = "I agree to Terms & Conditions and Privacy Policy",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Create Account Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        authViewModel.signUp(
                                            fullName = signUpFullName,
                                            businessName = signUpBusinessName,
                                            mobile = signUpMobile,
                                            rawEmail = signUpEmail,
                                            password = signUpPassword,
                                            confirmPassword = signUpConfirmPassword,
                                            agreeToTerms = agreeToTerms
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("signup_submit_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = SoftWhite
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading && !isSocialLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = SoftWhite,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "CREATE ACCOUNT & START",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Create",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Bottom Switch Link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Already have an account? ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Login",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldPrimary,
                                        modifier = Modifier.clickable {
                                            selectedTab = AuthTab.LOGIN
                                            errorMessage = null
                                            authViewModel.resetState()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trust & Security footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Secured",
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bank-grade 256-bit Cloud Encryption • Firebase Auth",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }

    // ==========================================
    // Forgot Password Dialog
    // ==========================================
    if (showForgotPasswordDialog) {
        val passwordResetState by authViewModel.passwordResetState.collectAsStateWithLifecycle()
        var resetEmail by remember { mutableStateOf(loginIdentifier.trim()) }

        Dialog(onDismissRequest = {
            if (passwordResetState !is PasswordResetState.Loading) {
                authViewModel.resetPasswordResetState()
                showForgotPasswordDialog = false
            }
        }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(LightMint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = "Reset",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter your registered email and we will send you a password recovery link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Your Email Address") },
                        placeholder = { Text("user@company.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = EmeraldPrimary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (val state = passwordResetState) {
                        is PasswordResetState.Success -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark,
                                textAlign = TextAlign.Center
                            )
                        }
                        is PasswordResetState.Error -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDC2626),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                authViewModel.resetPasswordResetState()
                                showForgotPasswordDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = passwordResetState !is PasswordResetState.Loading
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                authViewModel.sendPasswordReset(resetEmail)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            enabled = passwordResetState !is PasswordResetState.Loading
                        ) {
                            if (passwordResetState is PasswordResetState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = SoftWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send Link")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No auto-sign-in credentials found on device: ${e.message}")
            onUnauthenticated()
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts or credentials found on device: ${e.message}", e)
            onAuthError("No Google account selected or credentials found on device.")
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear credential state", e)
        }
        onSignOutComplete()
    }
}

@Composable
fun PasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = remember(password) { PasswordUtil.evaluate(password) }

    val meterColor by animateColorAsState(
        targetValue = when (strength.score) {
            1 -> Color(0xFFEF4444) // Red for Weak
            2 -> Color(0xFFF59E0B) // Amber for Fair
            3 -> Color(0xFF10B981) // Emerald for Good
            4 -> Color(0xFF059669) // Deep Emerald for Strong
            else -> Color(0xFF94A3B8) // Muted gray when empty
        },
        animationSpec = tween(300),
        label = "meterColorAnim"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength:",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            AnimatedContent(
                targetState = strength,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "strengthLabel"
            ) { targetStrength ->
                Text(
                    text = if (password.isNotEmpty()) targetStrength.label else "Too short",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (password.isNotEmpty()) meterColor else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4 segmented progress bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (step in 1..4) {
                val isFilled = strength.score >= step
                val barColor by animateColorAsState(
                    targetValue = if (isFilled) meterColor else Color(0xFFE2E8F0),
                    animationSpec = tween(250),
                    label = "barColor$step"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Real-time requirement criteria badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PasswordRequirementChip(
                label = "8+ chars",
                satisfied = strength.hasMinLength,
                modifier = Modifier.weight(1f)
            )
            PasswordRequirementChip(
                label = "A-Z",
                satisfied = strength.hasUppercase,
                modifier = Modifier.weight(1f)
            )
            PasswordRequirementChip(
                label = "a-z",
                satisfied = strength.hasLowercase,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PasswordRequirementChip(
                label = "0-9 Number",
                satisfied = strength.hasDigit,
                modifier = Modifier.weight(1f)
            )
            PasswordRequirementChip(
                label = "Symbol (@$!%)",
                satisfied = strength.hasSpecialChar,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PasswordRequirementChip(
    label: String,
    satisfied: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (satisfied) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
        animationSpec = tween(200),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (satisfied) Color(0xFF059669) else Color(0xFF94A3B8),
        animationSpec = tween(200),
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (satisfied) Color(0xFFA7F3D0) else Color(0xFFE2E8F0),
        animationSpec = tween(200),
        label = "chipBorder"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (satisfied) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (satisfied) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

fun performQuickInstantAccess(
    context: Context,
    coroutineScope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val quickEmail = "owner.demo@hisabnikash.com"
    val quickPass = "HisabNikash2026!"

    coroutineScope.launch {
        try {
            try {
                Firebase.auth.signInWithEmailAndPassword(quickEmail, quickPass).await()
            } catch (e: Exception) {
                val authResult = Firebase.auth.createUserWithEmailAndPassword(quickEmail, quickPass).await()
                val uid = authResult.user?.uid
                if (uid != null) {
                    val repository = FirestoreRepository(context)
                    repository.saveUserProfile(
                        UserProfile(
                            id = uid,
                            displayName = "Imran Business Owner",
                            businessName = "Hisab Nikash Enterprise",
                            phone = "01711002233",
                            email = quickEmail,
                            currency = "BDT"
                        )
                    )
                    repository.initializeDefaultDataIfEmpty()
                }
            }
            onAuthSuccess()
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Instant access failed")
        }
    }
}
