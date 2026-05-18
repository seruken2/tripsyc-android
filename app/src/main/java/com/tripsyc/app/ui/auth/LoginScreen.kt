package com.tripsyc.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.R
import com.tripsyc.app.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onOtpSent: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    var isEmailFocused by remember { mutableStateOf(false) }

    // Navigate when OTP is sent
    LaunchedEffect(state.otpSent) {
        if (state.otpSent) {
            onOtpSent(state.email.trim().lowercase())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Chalk50)
    ) {
        // ── Hero Section ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Coral,
                            Color(0xFFB84020)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = (-100).dp, y = (-70).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .offset(x = 120.dp, y = 50.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.04f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 80.dp, y = (-90).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // App icon
                Image(
                    painter = painterResource(R.drawable.tripsyc_icon),
                    contentDescription = "Tripsyc",
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(elevation = 20.dp, shape = RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )

                // Wordmark
                Image(
                    painter = painterResource(R.drawable.tripsyc_logo),
                    contentDescription = "Tripsyc",
                    modifier = Modifier
                        .height(32.dp)
                        .widthIn(max = 148.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentScale = ContentScale.Fit
                )

                // Tagline
                Text(
                    text = "Plan trips together, without the chaos",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Form Section (slides up from bottom, card style) ─────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Chalk50)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Sheet handle
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Chalk200)
                        .align(Alignment.CenterHorizontally)
                )

                // Sign in heading
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Sign in",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Chalk900
                    )
                    Text(
                        text = "No password needed — we'll email you a 6-digit code.",
                        fontSize = 14.sp,
                        color = Chalk500
                    )
                }

                // Email field + send button
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Email field with envelope icon
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(emailFocusRequester)
                            .onFocusChanged { isEmailFocused = it.isFocused },
                        placeholder = {
                            Text(
                                "your@email.com",
                                color = Chalk400
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = if (isEmailFocused) Coral else Chalk400
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.sendOtp()
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Coral,
                            unfocusedBorderColor = Chalk200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Chalk900,
                            unfocusedTextColor = Chalk900
                        )
                    )

                    // Error message
                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = Danger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Send Code button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.sendOtp()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Coral,
                            contentColor = Color.White,
                            disabledContainerColor = Coral.copy(alpha = 0.75f),
                            disabledContentColor = Color.White
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Send Code",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // ── OR divider ─────────────────────────────────
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Chalk200
                        )
                        Text("OR", color = Chalk400, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Chalk200
                        )
                    }

                    // Google Sign-In — opens the web OAuth dance in
                    // the default browser, which redirects back as
                    // tripsyc://google-auth?session=... once consent
                    // is granted. MainActivity unpacks the session
                    // into the cookie jar.
                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val base = com.tripsyc.app.data.api.ApiClient.BASE_URL.trimEnd('/')
                            val url = "$base/api/auth/google?mobile=1"
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url)
                            )
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Chalk900
                        )
                    ) {
                        Text("G", fontWeight = FontWeight.Bold, color = Coral, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Continue with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Terms notice
                val termsAnnotated = buildAnnotatedString {
                    withStyle(SpanStyle(color = Chalk400, fontSize = 11.sp)) {
                        append("By continuing, you agree to Tripsyc's\n")
                    }
                    withStyle(SpanStyle(
                        color = Coral,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append("Terms of Service")
                    }
                    withStyle(SpanStyle(color = Chalk400, fontSize = 11.sp)) {
                        append(" and ")
                    }
                    withStyle(SpanStyle(
                        color = Coral,
                        fontSize = 11.sp,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append("Privacy Policy")
                    }
                    withStyle(SpanStyle(color = Chalk400, fontSize = 11.sp)) {
                        append(".")
                    }
                }
                Text(
                    text = termsAnnotated,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TrustPill(icon: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 10.sp)
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Chalk500
        )
    }
}
