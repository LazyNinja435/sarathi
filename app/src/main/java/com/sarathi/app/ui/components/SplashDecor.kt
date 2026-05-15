package com.sarathi.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarathi.app.R
import com.sarathi.app.ui.theme.DeepBlue
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.MidnightIndigo
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Additional static ambience on the welcome screen (stars, vignette, feather hints, corners).
 * Drawn above [SacredBackground] so the rest of the app stays unchanged.
 */
@Composable
fun SplashScreenAtmosphere(modifier: Modifier = Modifier) {
    val featherPhase by rememberSharedLoopPhase(durationMillis = 15_200, offsetMillis = 800)
    val chakraPhase by rememberSharedLoopPhase(durationMillis = 70_000)
    val featherSway = -1f + triangleWave(featherPhase) * 2f
    val chakraRotation = chakraPhase * 360f
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        MidnightIndigo.copy(alpha = 0.55f),
                        DeepBlue.copy(alpha = 0.85f),
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = kotlin.math.max(w, h) * 0.95f,
                ),
            )
            val rnd = Random(20260514L)
            repeat(96) {
                val x = rnd.nextFloat() * w
                val y = rnd.nextFloat() * h
                val a = 0.05f + rnd.nextFloat() * 0.13f
                drawCircle(
                    color = SoftGold.copy(alpha = a),
                    radius = 0.6f + rnd.nextFloat() * 1.9f,
                    center = Offset(x, y),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.splash_corner_chakra_top_left),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-146).dp, y = (-138).dp)
                .size(252.dp)
                .graphicsLayer {
                    rotationZ = chakraRotation
                    alpha = 0.34f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.splash_corner_chakra_bottom_right),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 130.dp, y = 138.dp)
                .size(298.dp)
                .graphicsLayer {
                    rotationZ = chakraRotation
                    alpha = 0.34f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.splash_feather_top_right),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = 34.dp)
                .size(width = 178.dp, height = 284.dp)
                .graphicsLayer {
                    rotationZ = featherSway * 0.95f
                    alpha = 0.82f
                    transformOrigin = TransformOrigin(0.18f, 0.86f)
                },
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.splash_feather_bottom_left),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = (-172).dp)
                .size(width = 238.dp, height = 132.dp)
                .graphicsLayer {
                    rotationZ = -featherSway * 0.65f
                    alpha = 0.72f
                    transformOrigin = TransformOrigin(0.16f, 0.72f)
                },
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * Dimmer spark streams from the corner feathers so they echo the central emblem without competing.
 */
@Composable
fun SplashCornerFeatherParticleStreams(
    modifier: Modifier = Modifier,
    seed: Int = 20260515,
    particleCount: Int = 62,
) {
    val phase by rememberSharedLoopPhase(durationMillis = 16_500, offsetMillis = seed.toLong())
    Canvas(modifier) {
        drawFeatherParticleStream(
            source = Offset(size.width * 0.86f, size.height * 0.16f),
            phase = phase,
            seed = seed,
            particleCount = particleCount,
            directionBias = PI.toFloat() * 0.74f,
            radiusScale = 0.32f,
            alphaScale = 0.5f,
        )
        drawFeatherParticleStream(
            source = Offset(size.width * 0.16f, size.height * 0.72f),
            phase = (phase + 0.34f) % 1f,
            seed = seed + 97,
            particleCount = particleCount,
            directionBias = -PI.toFloat() * 0.18f,
            radiusScale = 0.28f,
            alphaScale = 0.42f,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFeatherParticleStream(
    source: Offset,
    phase: Float,
    seed: Int,
    particleCount: Int,
    directionBias: Float,
    radiusScale: Float,
    alphaScale: Float,
) {
    val rnd = Random(seed.toLong())
    repeat(particleCount) { index ->
        val stagger = index / particleCount.toFloat()
        val t = (phase + stagger) % 1f
        val drift = t.pow(0.76f)
        val spread = (rnd.nextFloat() - 0.5f) * PI.toFloat() * 0.72f
        val breath = sin((phase * 1.25f + stagger).toDouble() * PI * 2.0).toFloat()
        val angle = directionBias + spread + breath * 0.08f
        val maxRadius = size.width * (0.14f + rnd.nextFloat() * radiusScale)
        val radiusFromSource = 8f + drift * maxRadius
        val x = source.x + cos(angle) * radiusFromSource + breath * size.width * 0.012f
        val y = source.y + sin(angle) * radiusFromSource + drift * size.height * (0.012f + rnd.nextFloat() * 0.035f)
        val fadeIn = kotlin.math.min(1f, t * 8f)
        val fadeOut = (1f - drift).pow(0.95f)
        val bigSpark = index % 23 == 0
        val dotRadius = if (bigSpark) 2.2f + rnd.nextFloat() * 2.2f else 0.8f + rnd.nextFloat() * 1.8f
        val alpha = (if (bigSpark) 0.36f else 0.16f + rnd.nextFloat() * 0.2f) * fadeIn * fadeOut * alphaScale
        if (x >= -dotRadius && x <= size.width + dotRadius && y >= -dotRadius && y <= size.height + dotRadius) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SoftGold.copy(alpha = alpha),
                        SacredGold.copy(alpha = alpha * 0.42f),
                        Color.Transparent,
                    ),
                    center = Offset(x, y),
                    radius = dotRadius * if (bigSpark) 5.4f else 4.2f,
                ),
                radius = dotRadius * if (bigSpark) 5.4f else 4.2f,
                center = Offset(x, y),
            )
            drawCircle(
                color = SoftGold.copy(alpha = alpha * 0.8f),
                radius = dotRadius,
                center = Offset(x, y),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFeatherSilhouetteHint(
    w: Float,
    h: Float,
    flip: Boolean,
    origin: Offset,
) {
    val path = Path().apply {
        val sign = if (flip) -1f else 1f
        moveTo(origin.x, origin.y)
        quadraticTo(
            origin.x - sign * w * 0.22f,
            origin.y + sign * h * 0.18f,
            origin.x - sign * w * 0.08f,
            origin.y + sign * h * 0.42f,
        )
        quadraticTo(
            origin.x - sign * w * 0.02f,
            origin.y + sign * h * 0.28f,
            origin.x,
            origin.y,
        )
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                SacredGold.copy(alpha = 0.04f),
                Color.Transparent,
            ),
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBracket(
    corner: Offset,
    len: Float,
    topLeft: Boolean,
) {
    val c = SacredGold.copy(alpha = 0.12f)
    val sw = 1.2f
    if (topLeft) {
        drawLine(c, corner, corner + Offset(len, 0f), sw)
        drawLine(c, corner, corner + Offset(0f, len), sw)
        val flourish = 10f
        drawLine(c, corner + Offset(len * 0.85f, 0f), corner + Offset(len * 0.85f - flourish, flourish), sw)
        drawLine(c, corner + Offset(0f, len * 0.85f), corner + Offset(flourish, len * 0.85f - flourish), sw)
    } else {
        drawLine(c, corner, corner + Offset(-len, 0f), sw)
        drawLine(c, corner, corner + Offset(0f, -len), sw)
        val flourish = 10f
        drawLine(c, corner + Offset(-len * 0.85f, 0f), corner + Offset(-len * 0.85f + flourish, -flourish), sw)
        drawLine(c, corner + Offset(0f, -len * 0.85f), corner + Offset(-flourish, -len * 0.85f + flourish), sw)
    }
}

/**
 * Soft light motes drifting outward from the canvas center; tuned for low particle count.
 */
@Composable
fun SplashRadianceMotes(
    modifier: Modifier = Modifier,
    seed: Int = 41,
    particleCount: Int = 14,
) {
    val phase by rememberSharedLoopPhase(durationMillis = 10_000, offsetMillis = seed.toLong())
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rnd = Random(seed.toLong())
        val angles = FloatArray(particleCount) { rnd.nextFloat() * 2f * PI.toFloat() }
        val maxR = size.minDimension * 0.48f
        for (i in 0 until particleCount) {
            val stagger = (i.toFloat() / particleCount)
            val t = (phase + stagger) % 1f
            val ease = t.pow(0.85f)
            val ang = angles[i] + sin(phase.toDouble() * PI * 2.0).toFloat() * 0.04f
            val r = 18f + ease * maxR
            val alpha = (1f - ease).pow(1.4f) * 0.38f
            val shimmer = 0.85f + 0.15f * sin((phase + i * 0.13f).toDouble() * PI * 2.0).toFloat()
            val px = cx + cos(ang) * r
            val py = cy + sin(ang) * r
            val pr = 1f + (1f - ease) * 1.1f
            drawCircle(
                color = SoftGold.copy(alpha = alpha * shimmer),
                radius = pr,
                center = Offset(px, py),
            )
            drawCircle(
                color = SacredGold.copy(alpha = alpha * 0.35f * shimmer),
                radius = pr * 0.45f,
                center = Offset(px - 0.3f, py - 0.3f),
            )
        }
    }
}

/**
 * Slow gold motes that begin at the hero feather and drift across the full welcome screen.
 */
@Composable
fun SplashLogoParticleStream(
    modifier: Modifier = Modifier,
    seed: Int = 20260514,
    particleCount: Int = 132,
) {
    val phase by rememberSharedLoopPhase(durationMillis = 14_000, offsetMillis = seed.toLong())
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val source = Offset(w * 0.5f, h * 0.29f)
        val rnd = Random(seed.toLong())
        repeat(particleCount) { index ->
            val stagger = index / particleCount.toFloat()
            val t = (phase + stagger) % 1f
            val baseAngle = rnd.nextFloat() * 2f * PI.toFloat()
            val breath = sin((phase * 1.4f + stagger).toDouble() * PI * 2.0).toFloat()
            val angle = baseAngle + breath * (0.08f + rnd.nextFloat() * 0.1f)
            val maxRadius = w * (0.18f + rnd.nextFloat() * 0.46f)
            val drift = t.pow(0.72f)
            val currentRadius = 10f + drift * maxRadius
            val downwardPull = drift * h * (0.015f + rnd.nextFloat() * 0.08f)
            val x = source.x + cos(angle) * currentRadius + breath * w * 0.018f
            val y = source.y + sin(angle) * currentRadius + downwardPull
            val fadeIn = kotlin.math.min(1f, t * 7f)
            val fadeOut = (1f - drift).pow(0.86f)
            val bigSpark = index % 17 == 0 || index % 29 == 0
            val radius = if (bigSpark) {
                2.5f + rnd.nextFloat() * 4.2f
            } else {
                0.9f + rnd.nextFloat() * 2.6f
            }
            val alpha = (if (bigSpark) 0.58f else 0.22f + rnd.nextFloat() * 0.36f) * fadeIn * fadeOut
            if (x >= -radius && x <= w + radius && y >= -radius && y <= h + radius) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SoftGold.copy(alpha = alpha),
                            SacredGold.copy(alpha = alpha * 0.46f),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius * if (bigSpark) 6.4f else 4.8f,
                    ),
                    radius = radius * if (bigSpark) 6.4f else 4.8f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = SoftGold.copy(alpha = alpha * 0.95f),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
        }
        repeat(20) { index ->
            val local = (phase + index / 20f) % 1f
            val angle = index * PI.toFloat() * 2f / 20f + phase * 0.35f
            val r = 20f + local * w * 0.28f
            val alpha = (1f - local).pow(1.55f) * 0.38f
            drawLine(
                color = SacredGold.copy(alpha = alpha * 0.38f),
                start = Offset(source.x + cos(angle) * r * 0.22f, source.y + sin(angle) * r * 0.22f),
                end = Offset(source.x + cos(angle) * r, source.y + sin(angle) * r),
                strokeWidth = 1f,
            )
        }
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

@Composable
fun SplashOrnamentalDivider(modifier: Modifier = Modifier) {
    SplashDividerAsset(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 8.dp),
        alpha = 0.5f,
    )
}

@Composable
fun SplashLotusTitleRow(modifier: Modifier = Modifier) {
    SplashDividerAsset(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(modifier),
        alpha = 0.56f,
    )
}

@Composable
fun SplashBeginButtonWithFiligree(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(74.dp)
            .widthIn(max = 560.dp),
        contentAlignment = Alignment.Center,
    ) {
        SacredButton(onClick = onClick) {
            Text(
                text = "Begin",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Ink,
            )
        }
    }
}

@Composable
fun SplashFooterLotus(modifier: Modifier = Modifier) {
    SplashDividerAsset(
        modifier
            .fillMaxWidth()
            .height(22.dp),
        alpha = 0.32f,
    )
}

@Composable
private fun SplashDividerAsset(
    modifier: Modifier,
    alpha: Float,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.splash_divider_lotus),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
            contentScale = ContentScale.Fit,
        )
    }
}
