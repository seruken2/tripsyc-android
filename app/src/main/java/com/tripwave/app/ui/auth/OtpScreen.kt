package com.tripwave.app.ui.auth

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripwave.app.R
import com.tripwave.app.data.api.models.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Boarding-pass OTP verification screen.
 *
 * The verification screen styled as an airline boarding pass — the
 * user is "boarding" their Tripwave session. A coral header strip
 * carries the Tripwave logo; below it a route row (HOM -> TRP), a
 * dashed perforation, six boxed code slots and an ink "Board now"
 * button. Wired to the existing AuthViewModel (/api/auth/verify-otp).
 */

// Boarding-pass palette.
private val BpChalkWarm = Color(0xFFFAF4EC)
private val BpCoral = Color(0xFFE8654A)
private val BpInk = Color(0xFF2B2825)
private val BpInkFaint = Color(0xFF8B847C)
private val BpEdge = Color(0xFFE8DDD2)
private val BpError = Color(0xFFC9402E)
private val BpPage = Color(0xFFF5F2EE)

private const val RESEND_SECONDS = 45
private val ROUTE = listOf("FROM" to "HOM", "GATE" to "B6", "TO" to "TRP")

@Composable
fun OtpScreen(
    email: String,
    viewModel: AuthViewModel,
    onVerified: (User) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val digits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    var focusedIndex by remember { mutableStateOf(-1) }
    var verified by remember { mutableStateOf(false) }
    var resendLeft by remember { mutableStateOf(RESEND_SECONDS) }
    var resendTick by remember { mutableStateOf(0) }
    var flashError by remember { mutableStateOf(false) }
    val shakeX = remember { Animatable(0f) }

    val code = digits.joinToString("")
    val busy = state.isVerifying || verified
    val lift by animateDpAsState(if (verified) (-4).dp else 0.dp, label = "lift")

    fun syncCode() {
        viewModel.updateCode(digits.joinToString(""))
    }

    fun verify() {
        if (state.isVerifying || verified) return
        viewModel.verifyCode { user ->
            verified = true
            // Brief "✓ Boarded" beat before navigating onward.
            scope.launch {
                delay(600)
                onVerified(user)
            }
        }
    }

    fun distribute(raw: String, start: Int) {
        var idx = start
        for (ch in raw) {
            if (idx > 5) break
            digits[idx] = ch.toString()
            idx += 1
        }
        syncCode()
        focusRequesters[(if (idx > 5) 5 else idx).coerceIn(0, 5)].requestFocus()
    }

    fun onChange(i: Int, raw: String) {
        val v = raw.filter { it.isDigit() }
        if (v.length > 1) {
            distribute(v, i)
            return
        }
        digits[i] = v
        syncCode()
        if (v.isNotEmpty() && i < 5) focusRequesters[i + 1].requestFocus()
    }

    fun onResend() {
        if (resendLeft > 0 || busy) return
        viewModel.sendOtp()
        resendTick += 1
        for (i in 0..5) digits[i] = ""
        syncCode()
        focusRequesters[0].requestFocus()
        Toast.makeText(context, "New code sent", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    LaunchedEffect(resendTick) {
        resendLeft = RESEND_SECONDS
        while (resendLeft > 0) {
            delay(1000)
            resendLeft -= 1
        }
    }

    LaunchedEffect(code) {
        if (code.length == 6) verify()
    }

    // On a rejected code: shake, flash the slot borders red, clear, refocus.
    LaunchedEffect(state.error) {
        if (state.error != null) {
            flashError = true
            for (i in 0..5) digits[i] = ""
            focusRequesters[0].requestFocus()
            shakeX.snapTo(0f)
            for (target in listOf(-7f, 7f, -5f, 5f, 0f)) {
                shakeX.animateTo(target, animationSpec = tween(80))
            }
            delay(40)
            flashError = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BpPage),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Boarding pass ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .offset(y = lift)
                    .graphicsLayer { translationX = shakeX.value }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, BpEdge, RoundedCornerShape(8.dp)),
            ) {
                // Coral header strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BpCoral)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        // Tripwave logo on a boarding-pass-style white badge.
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(5.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.tripwave_logo),
                                contentDescription = "Tripwave",
                                modifier = Modifier.height(15.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "Boarding pass",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = Color.White,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (verified) 0f else -15f),
                    )
                }

                // Body
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                    // Route row — decorative airline flavour.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for ((label, value) in ROUTE) {
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp,
                                    color = BpInkFaint,
                                )
                                Text(
                                    text = value,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BpInk,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    DashedDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "ENTER BOARDING CODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp,
                        color = BpInkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("Sent to ")
                            withStyle(SpanStyle(color = BpInk, fontWeight = FontWeight.Medium)) {
                                append(email)
                            }
                        },
                        fontSize = 11.sp,
                        color = BpInkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    // ── Six boxed slots ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        for (i in 0..5) {
                            DigitCell(
                                value = digits[i],
                                enabled = !busy,
                                focused = focusedIndex == i,
                                flashError = flashError,
                                focusRequester = focusRequesters[i],
                                onValueChange = { onChange(i, it) },
                                onFocusedChange = { if (it) focusedIndex = i },
                                onBackspaceWhenEmpty = {
                                    if (i > 0) {
                                        digits[i - 1] = ""
                                        syncCode()
                                        focusRequesters[i - 1].requestFocus()
                                    }
                                },
                            )
                        }
                    }

                    if (state.error != null && !verified) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.error ?: "",
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = BpError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // ── Board now ──
                    Spacer(Modifier.height(18.dp))
                    val canSubmit = code.length == 6 && !busy
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BpInk)
                            .alpha(if (code.length < 6 && !busy) 0.4f else 1f)
                            .clickable(enabled = canSubmit) { verify() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            }
                            Text(
                                text = when {
                                    verified -> "✓ Boarded"
                                    state.isVerifying -> "Boarding…"
                                    else -> "Board now ✈"
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // ── Footer (outside the card) ──
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (resendLeft > 0) {
                    Text(
                        text = "Resend in 0:${resendLeft.toString().padStart(2, '0')}",
                        fontSize = 12.sp,
                        color = BpInkFaint,
                    )
                } else {
                    Text(
                        text = "Resend code",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BpCoral,
                        modifier = Modifier.clickable(enabled = !busy) { onResend() },
                    )
                }
                Text("  ·  ", fontSize = 12.sp, color = BpInkFaint)
                Text(
                    text = "Go back",
                    fontSize = 12.sp,
                    color = BpInk,
                    modifier = Modifier.clickable { onBack() },
                )
            }
        }
    }
}

// ── Single boxed digit slot ──
@Composable
private fun DigitCell(
    value: String,
    enabled: Boolean,
    focused: Boolean,
    flashError: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onFocusedChange: (Boolean) -> Unit,
    onBackspaceWhenEmpty: () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BpInk,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(BpCoral),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .width(36.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusedChange(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Backspace &&
                    event.type == KeyEventType.KeyDown &&
                    value.isEmpty()
                ) {
                    onBackspaceWhenEmpty()
                    true
                } else {
                    false
                }
            },
        decorationBox = { inner ->
            val borderColor = when {
                flashError -> BpError
                focused -> BpCoral
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (focused) Color.White else BpChalkWarm)
                    .border(1.5.dp, borderColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                inner()
            }
        },
    )
}

// ── Dashed perforation hairline ──
@Composable
private fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        val y = size.height / 2f
        drawLine(
            color = BpEdge,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)),
        )
    }
}
