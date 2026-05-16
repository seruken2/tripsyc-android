package com.tripsyc.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.tripsyc.app.ui.theme.Coral
import com.tripsyc.app.ui.theme.Dusk
import com.tripsyc.app.ui.theme.Gold
import com.tripsyc.app.ui.theme.Sage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One-shot confetti overlay for trip-creation / lock-in celebrations.
 * Particles spawn at the burst origin, fan outwards with gravity, and
 * fade as the animation progresses. Pass `trigger=true` to run; the
 * caller controls re-runs by toggling the bool.
 */
@Composable
fun ConfettiBurst(
    trigger: Boolean,
    particleCount: Int = 80,
    durationMillis: Int = 1400,
    modifier: Modifier = Modifier
) {
    if (!trigger) return

    val palette = listOf(Coral, Gold, Sage, Dusk, Color(0xFFE85C90))

    // Random per-particle init values stay stable for the lifetime of
    // this composition (one burst). Recomposed only when `trigger` flips.
    val particles = remember(trigger) {
        List(particleCount) {
            ConfettiParticle(
                angle = Random.nextDouble(0.0, 2 * PI).toFloat(),
                speed = Random.nextFloat() * 380f + 220f,
                size = Random.nextFloat() * 6f + 4f,
                color = palette[Random.nextInt(palette.size)],
                rotationSpeed = Random.nextFloat() * 720f - 360f
            )
        }
    }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        started = true
    }

    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
        label = "confetti-progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val origin = Offset(size.width / 2f, size.height * 0.30f)
        val gravity = 800f * progress * progress

        particles.forEach { p ->
            val dx = cos(p.angle) * p.speed * progress
            val dy = sin(p.angle) * p.speed * progress + gravity
            val alpha = 1f - progress
            val pos = Offset(origin.x + dx, origin.y + dy)
            drawCircle(
                color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = p.size,
                center = pos
            )
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)
