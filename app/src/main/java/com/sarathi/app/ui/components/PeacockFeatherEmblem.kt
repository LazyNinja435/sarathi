package com.sarathi.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sarathi.app.R
import com.sarathi.app.ui.theme.DeepBlue
import com.sarathi.app.ui.theme.PeacockGreen
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class PeacockEmblemStyle {
    /** Smaller emblem for headers and secondary screens. */
    Standard,

    /** Splash / hero: mandala halo, pendant, layered glow, subtle motion. */
    Hero,
}

@Composable
fun PeacockFeatherEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    style: PeacockEmblemStyle = PeacockEmblemStyle.Standard,
) {
    when (style) {
        PeacockEmblemStyle.Standard -> StandardPeacockEmblemCanvas(modifier.size(size))
        PeacockEmblemStyle.Hero -> HeroPeacockEmblemCanvas(modifier.size(size))
    }
}

@Composable
private fun StandardPeacockEmblemCanvas(modifier: Modifier) {
    Canvas(modifier) {
        drawStandardEmblem()
    }
}

@Composable
private fun HeroPeacockEmblemCanvas(modifier: Modifier) {
    val pulsePhase by rememberSharedLoopPhase(durationMillis = 8400)
    val swayPhase by rememberSharedLoopPhase(durationMillis = 12_400, offsetMillis = 1100)
    val pulse = 0.86f + triangleWave(pulsePhase) * 0.14f
    val sway = -1f + triangleWave(swayPhase) * 2f
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.splash_back_ring_chakra),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.9f + pulse * 0.1f
                    scaleX = 0.99f + pulse * 0.012f
                    scaleY = 0.99f + pulse * 0.012f
                },
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.splash_central_peacock_feather),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 14.dp)
                .graphicsLayer {
                    rotationZ = sway * 1.2f
                    scaleX = 1f + kotlin.math.abs(sway) * 0.01f
                    scaleY = 1f - kotlin.math.abs(sway) * 0.006f
                },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun rememberSharedLoopPhase(
    durationMillis: Long,
    offsetMillis: Long = 0L,
) = produceState(0f, durationMillis, offsetMillis) {
    while (true) {
        withInfiniteAnimationFrameMillis { frameMillis ->
            value = ((frameMillis + offsetMillis) % durationMillis).toFloat() / durationMillis
        }
    }
}

private fun triangleWave(phase: Float): Float =
    if (phase < 0.5f) phase * 2f else (1f - phase) * 2f

private fun DrawScope.drawStandardEmblem() {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SacredGold.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(cx, cy),
            radius = w * 0.55f,
        ),
        radius = w * 0.55f,
        center = Offset(cx, cy),
    )
    val feather = featherPath(cx, cy, w, h)
    drawPath(
        path = feather,
        brush = Brush.verticalGradient(
            colors = listOf(
                DeepBlue.copy(alpha = 0.9f),
                PeacockGreen.copy(alpha = 0.85f),
            ),
        ),
    )
    drawPath(
        path = feather,
        color = SacredGold.copy(alpha = 0.55f),
        style = Stroke(width = 2f),
    )
    drawOval(
        color = PeacockGreen,
        topLeft = Offset(cx - w * 0.12f, cy - h * 0.02f),
        size = Size(w * 0.24f, h * 0.14f),
    )
    drawOval(
        color = DeepBlue,
        topLeft = Offset(cx - w * 0.05f, cy + h * 0.01f),
        size = Size(w * 0.1f, h * 0.08f),
    )
    drawCircle(
        color = SacredGold.copy(alpha = 0.35f),
        radius = w * 0.42f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f),
    )
    val rays = 18
    val r0 = w * 0.22f
    val r1 = w * 0.4f
    for (i in 0 until rays) {
        val ang = (PI * 2 * i / rays).toFloat()
        drawLine(
            color = SoftGold.copy(alpha = 0.12f),
            start = Offset(cx + cos(ang) * r0, cy + sin(ang) * r0),
            end = Offset(cx + cos(ang) * r1, cy + sin(ang) * r1),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawHeroReferenceHalo(pulse: Float, haloPhase: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.43f
    val strokeFaint = Stroke(width = 1.1f)
    val mandalaAlpha = 0.1f + pulse * 0.04f
    for (ring in 1..6) {
        drawCircle(
            color = SacredGold.copy(alpha = mandalaAlpha * (1f - ring * 0.13f)),
            radius = w * (0.11f + ring * 0.069f),
            center = Offset(cx, cy),
            style = strokeFaint,
        )
    }
    val spokeRot = haloPhase * (PI.toFloat() / 30f)
    repeat(28) { s ->
        val ang = (PI * 2 * s / 28).toFloat() + spokeRot
        drawLine(
            color = SacredGold.copy(alpha = 0.045f),
            start = Offset(cx, cy),
            end = Offset(cx + cos(ang) * w * 0.42f, cy + sin(ang) * w * 0.42f),
            strokeWidth = 0.8f,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SoftGold.copy(alpha = 0.34f * pulse),
                SacredGold.copy(alpha = 0.18f * pulse),
                Color.Transparent,
            ),
            center = Offset(cx, cy + h * 0.02f),
            radius = w * 0.5f,
        ),
        radius = w * 0.5f,
        center = Offset(cx, cy + h * 0.02f),
    )
}

private fun DrawScope.drawHeroEmblem(pulse: Float, haloPhase: Float, sway: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.38f
    val strokeFaint = Stroke(width = 1.1f)
    val mandalaAlpha = 0.12f + pulse * 0.06f
    for (ring in 1..6) {
        drawCircle(
            color = SacredGold.copy(alpha = mandalaAlpha * (1f - ring * 0.12f)),
            radius = w * (0.1f + ring * 0.072f),
            center = Offset(cx, cy),
            style = strokeFaint,
        )
    }
    val spokeRot = haloPhase * (PI.toFloat() / 28f)
    val spokes = 24
    val rSpoke = w * 0.38f
    for (s in 0 until spokes) {
        val ang = (PI * 2 * s / spokes).toFloat() + spokeRot
        drawLine(
            color = SacredGold.copy(alpha = 0.06f),
            start = Offset(cx, cy),
            end = Offset(cx + cos(ang) * rSpoke, cy + sin(ang) * rSpoke),
            strokeWidth = 0.85f,
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SoftGold.copy(alpha = 0.5f * pulse),
                SacredGold.copy(alpha = 0.28f * pulse),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = w * 0.58f,
        ),
        radius = w * 0.58f,
        center = Offset(cx, cy),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SoftGold.copy(alpha = 0.58f * pulse), SacredGold.copy(alpha = 0.18f * pulse), Color.Transparent),
            center = Offset(cx, cy + h * 0.06f),
            radius = w * 0.28f,
        ),
        radius = w * 0.38f,
        center = Offset(cx, cy + h * 0.04f),
    )
    drawCircle(
        color = SacredGold.copy(alpha = 0.34f + 0.1f * pulse),
        radius = w * 0.37f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.4f),
    )
    val rays = 28
    val r0 = w * 0.2f
    val r1 = w * 0.46f
    for (i in 0 until rays) {
        val ang = (PI * 2 * i / rays).toFloat() + spokeRot * 0.5f
        drawLine(
            color = SoftGold.copy(alpha = 0.08f + 0.04f * pulse),
            start = Offset(cx + cos(ang) * r0, cy + sin(ang) * r0),
            end = Offset(cx + cos(ang) * r1, cy + sin(ang) * r1),
            strokeWidth = 1.1f,
        )
    }
    rotate(degrees = sway * 1.35f, pivot = Offset(cx, cy + h * 0.17f)) {
        drawFlameFeatherLogo(cx = cx, cy = cy, w = w, h = h, pulse = pulse, sway = sway)
    }
}

private fun DrawScope.drawFlameFeatherLogo(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    pulse: Float,
    sway: Float,
) {
    val tip = Offset(cx + sway * w * 0.012f, cy - h * 0.34f)
    val base = Offset(cx, cy + h * 0.36f)
    val leftFlame = flameSidePath(
        tip = tip,
        base = base,
        outer = Offset(cx - w * 0.28f, cy + h * 0.02f),
        inner = Offset(cx - w * 0.09f, cy + h * 0.17f),
    )
    val rightFlame = flameSidePath(
        tip = tip,
        base = base,
        outer = Offset(cx + w * 0.28f, cy + h * 0.02f),
        inner = Offset(cx + w * 0.09f, cy + h * 0.17f),
    )
    drawPath(
        path = leftFlame,
        brush = Brush.linearGradient(
            colors = listOf(
                SoftGold.copy(alpha = 0.12f),
                Color(0xFFFFC84E).copy(alpha = 0.76f),
                SoftGold.copy(alpha = 0.96f),
            ),
            start = Offset(cx - w * 0.2f, cy - h * 0.2f),
            end = base,
        ),
    )
    drawPath(
        path = rightFlame,
        brush = Brush.linearGradient(
            colors = listOf(
                SoftGold.copy(alpha = 0.12f),
                Color(0xFFFFC84E).copy(alpha = 0.76f),
                SoftGold.copy(alpha = 0.96f),
            ),
            start = Offset(cx + w * 0.2f, cy - h * 0.2f),
            end = base,
        ),
    )
    for (i in -5..5) {
        val t = i / 5f
        val strand = Path().apply {
            moveTo(cx + t * w * 0.022f, base.y - h * 0.01f)
            cubicTo(
                cx + t * w * 0.18f,
                cy + h * 0.12f,
                cx + t * w * 0.13f + sway * w * 0.018f,
                cy - h * 0.1f,
                tip.x + t * w * 0.018f,
                tip.y + h * 0.08f,
            )
        }
        drawPath(
            path = strand,
            color = SoftGold.copy(alpha = 0.35f + 0.22f * (1f - kotlin.math.abs(t))),
            style = Stroke(width = 1.4f + (1f - kotlin.math.abs(t)) * 0.8f),
        )
    }
    val centerLeaf = Path().apply {
        moveTo(cx, cy - h * 0.15f)
        cubicTo(cx + w * 0.18f, cy - h * 0.06f, cx + w * 0.16f, cy + h * 0.22f, cx, cy + h * 0.31f)
        cubicTo(cx - w * 0.16f, cy + h * 0.22f, cx - w * 0.18f, cy - h * 0.06f, cx, cy - h * 0.15f)
        close()
    }
    drawPath(
        path = centerLeaf,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE8D36F).copy(alpha = 0.98f),
                PeacockGreen.copy(alpha = 0.94f),
                Color(0xFF0D6F54).copy(alpha = 0.95f),
            ),
            startY = cy - h * 0.18f,
            endY = cy + h * 0.32f,
        ),
    )
    drawPath(
        path = centerLeaf,
        color = SacredGold.copy(alpha = 0.9f),
        style = Stroke(width = 2.1f),
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF2BD2C6), Color(0xFF006A75).copy(alpha = 0.9f)),
            center = Offset(cx, cy + h * 0.02f),
            radius = w * 0.18f,
        ),
        topLeft = Offset(cx - w * 0.15f, cy - h * 0.005f),
        size = Size(w * 0.3f, h * 0.14f),
    )
    drawOval(
        color = Color(0xFF021B49),
        topLeft = Offset(cx - w * 0.083f, cy + h * 0.018f),
        size = Size(w * 0.078f, h * 0.095f),
    )
    drawOval(
        color = Color(0xFF063C83),
        topLeft = Offset(cx + w * 0.005f, cy + h * 0.018f),
        size = Size(w * 0.078f, h * 0.095f),
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF10215F).copy(alpha = 0.94f), Color(0xFF06102E)),
            center = Offset(cx, cy + h * 0.07f),
            radius = w * 0.08f,
        ),
        topLeft = Offset(cx - w * 0.055f, cy + h * 0.038f),
        size = Size(w * 0.11f, h * 0.075f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SoftGold.copy(alpha = 0.94f), SacredGold.copy(alpha = 0.48f), Color.Transparent),
            center = base,
            radius = w * 0.08f,
        ),
        radius = w * 0.06f,
        center = base,
    )
    var beadY = base.y + h * 0.13f
    val beads = listOf(w * 0.028f, w * 0.02f, w * 0.012f)
    for (radius in beads) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SoftGold.copy(alpha = 0.9f), SacredGold.copy(alpha = 0.62f), Color.Transparent),
                center = Offset(cx, beadY),
                radius = radius * 1.9f,
            ),
            radius = radius,
            center = Offset(cx, beadY),
        )
        beadY += radius * 3f
    }
    drawCircle(
        color = SoftGold.copy(alpha = 0.78f + 0.12f * pulse),
        radius = w * 0.007f,
        center = Offset(cx, beadY + h * 0.018f),
    )
}

private fun flameSidePath(tip: Offset, base: Offset, outer: Offset, inner: Offset): Path = Path().apply {
    moveTo(tip.x, tip.y)
    cubicTo(outer.x * 0.78f + tip.x * 0.22f, tip.y + (outer.y - tip.y) * 0.16f, outer.x, outer.y, base.x, base.y)
    cubicTo(inner.x, inner.y + (base.y - inner.y) * 0.1f, tip.x + (inner.x - tip.x) * 0.18f, tip.y + (inner.y - tip.y) * 0.7f, tip.x, tip.y)
    close()
}

private fun featherPath(cx: Float, cy: Float, w: Float, h: Float): Path = Path().apply {
    moveTo(cx, cy - h * 0.02f)
    cubicTo(
        cx + w * 0.35f, cy + h * 0.05f,
        cx + w * 0.22f, cy + h * 0.42f,
        cx, cy + h * 0.48f,
    )
    cubicTo(
        cx - w * 0.22f, cy + h * 0.42f,
        cx - w * 0.35f, cy + h * 0.05f,
        cx, cy - h * 0.02f,
    )
    close()
}

private fun DrawScope.drawGoldStrands(cx: Float, cy: Float, w: Float, h: Float) {
    val strands = 7
    for (i in -strands / 2..strands / 2) {
        val t = i / (strands / 2f + 0.01f)
        val ox = t * w * 0.08f
        val path = Path().apply {
            moveTo(cx + ox * 0.2f, cy + h * 0.06f)
            quadraticTo(
                cx + ox * 1.2f + w * 0.04f * t,
                cy + h * 0.22f,
                cx + ox * 0.4f,
                cy + h * 0.4f,
            )
        }
        drawPath(
            path = path,
            color = SacredGold.copy(alpha = 0.14f + 0.06f * (1f - kotlin.math.abs(t))),
            style = Stroke(width = 1.2f),
        )
    }
}
