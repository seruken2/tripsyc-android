package com.tripsyc.app.ui.auth

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripsyc.app.data.api.models.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Postcard OTP verification screen.
 *
 * The verification code arrives like a stamped postcard in the mail —
 * a vintage-airmail card with a coral postage stamp, dashed dividers,
 * and the six digits typed onto signature-style underlines. Wired to
 * the existing AuthViewModel (custom /api/auth/verify-otp flow).
 */

// Postcard palette — a warm vintage-airmail look, deliberately distinct
// from the rest of the chalk UI.
private val Paper = Color(0xFFFFFEF9)
private val Ink = Color(0xFF2B2825)
private val InkMuted = Color(0xFF5F5E5A)
private val InkFaint = Color(0xFF8B847C)
private val EdgeColor = Color(0xFFD6CDC0)
private val PostcardCoral = Color(0xFFE8654A)
private val CoralDark = Color(0xFFC9512F)
private val ErrorRed = Color(0xFFC9402E)
private val PageBg = Color(0xFFF5F2EE)

private const val RESEND_SECONDS = 45

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

    // Mirror the digit cells into the view model so verifyCode() reads
    // the current code, and so a fresh keystroke clears any prior error.
    fun syncCode() {
        viewModel.updateCode(digits.joinToString(""))
    }

    fun verify() {
        if (state.isVerifying || verified) return
        viewModel.verifyCode { user ->
            verified = true
            // Brief "✓ Verified" beat before navigating onward.
            scope.launch {
                delay(600)
                onVerified(user)
            }
        }
    }

    fun setDigit(i: Int, value: String) {
        digits[i] = value
        syncCode()
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
        setDigit(i, v)
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

    // Focus the first cell on appear.
    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    // Resend cooldown — restarts whenever resendTick changes.
    LaunchedEffect(resendTick) {
        resendLeft = RESEND_SECONDS
        while (resendLeft > 0) {
            delay(1000)
            resendLeft -= 1
        }
    }

    // Auto-submit once all six digits are entered.
    LaunchedEffect(code) {
        if (code.length == 6) verify()
    }

    // On a rejected code: shake the card, flash the underlines red,
    // clear the cells, and refocus the first.
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
            .background(PageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Postcard ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .offset(y = lift)
                    .graphicsLayer { translationX = shakeX.value }
                    .background(Paper, RoundedCornerShape(4.dp))
                    .border(1.dp, Ink, RoundedCornerShape(4.dp))
                    .padding(24.dp),
            ) {
                // Label + stamp share the top row.
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "POSTCARD · TRIPSYC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color = InkFaint,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    Stamp(
                        verified = verified,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Greetings!",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildAnnotatedString {
                        append("Your code is waiting at\n")
                        withStyle(SpanStyle(color = CoralDark, fontWeight = FontWeight.Medium)) {
                            append(email)
                        }
                    },
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = InkMuted,
                )

                Spacer(Modifier.height(18.dp))
                DashedDivider()
                Spacer(Modifier.height(14.dp))

                Text(
                    text = "ENTER STAMP CODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    color = InkFaint,
                )
                Spacer(Modifier.height(10.dp))

                // ── Six digit cells ──
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            index = i,
                        )
                    }
                }

                // ── Error text ──
                if (state.error != null && !verified) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.error ?: "",
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = ErrorRed,
                    )
                }

                // ── Submit ──
                Spacer(Modifier.height(18.dp))
                val canSubmit = code.length == 6 && !busy
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PostcardCoral)
                        .alpha(if (code.length < 6 && !busy) 0.4f else 1f)
                        .clickable(enabled = canSubmit) { verify() },
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
                                verified -> "✓ Verified"
                                state.isVerifying -> "Verifying…"
                                else -> "Stamp & send"
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // ── Resend ──
                Spacer(Modifier.height(14.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (resendLeft > 0) {
                        Text(
                            text = "Resend in 0:${resendLeft.toString().padStart(2, '0')}",
                            fontSize = 11.sp,
                            color = InkFaint,
                        )
                    } else {
                        Text(
                            text = "Resend code",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PostcardCoral,
                            modifier = Modifier.clickable(enabled = !busy) { onResend() },
                        )
                    }
                }
            }

            // ── Footer (outside the card) ──
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wrong email? ", fontSize = 12.sp, color = InkFaint)
                Text(
                    text = "Go back",
                    fontSize = 12.sp,
                    color = Ink,
                    modifier = Modifier.clickable { onBack() },
                )
            }
        }
    }
}

// ── Single digit cell — courier digit on a signature-style underline ──
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
    index: Int,
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
            color = Ink,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(PostcardCoral),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .width(32.dp)
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
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(40.dp)
                    .drawBehind {
                        val underline = when {
                            flashError -> ErrorRed
                            value.isNotEmpty() -> Ink
                            focused -> PostcardCoral
                            else -> EdgeColor
                        }
                        val stroke = 1.5.dp.toPx()
                        val y = size.height - stroke
                        drawLine(
                            color = underline,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                inner()
            }
        },
    )
}

// ── Postage stamp — coral block, dashed perforation, paper plane ──
@Composable
private fun Stamp(verified: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp, 56.dp)
            .rotate(if (verified) 0f else -3f)
            .background(PostcardCoral, RoundedCornerShape(2.dp))
            .border(1.dp, PostcardCoral, RoundedCornerShape(2.dp))
            .drawBehind {
                val inset = 4.dp.toPx()
                drawRoundRect(
                    color = Paper,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(22.dp)
                .rotate(if (verified) 0f else -15f),
        )
    }
}

// ── Dashed divider hairline ──
@Composable
private fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        val y = size.height / 2f
        drawLine(
            color = EdgeColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
        )
    }
}
