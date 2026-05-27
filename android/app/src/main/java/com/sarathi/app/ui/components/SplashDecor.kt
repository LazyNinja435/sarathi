package com.sarathi.app.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
fun SplashScreenAtmosphere(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    SoftGold.copy(alpha = 0.08f),
                    MidnightIndigo.copy(alpha = 0.28f),
                    DeepBlue.copy(alpha = 0.82f),
                ),
                center = Offset(w * 0.5f, h * 0.43f),
                radius = kotlin.math.max(w, h) * 0.82f,
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    DeepBlue.copy(alpha = 0.48f),
                ),
                startY = h * 0.62f,
                endY = h,
            ),
        )
        val rnd = Random(20260517L)
        repeat(38) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val alpha = 0.05f + rnd.nextFloat() * 0.12f
            drawCircle(
                color = SoftGold.copy(alpha = alpha),
                radius = 0.8f + rnd.nextFloat() * 1.7f,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
fun SplashCenterLogo(
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val base = if (maxWidth < maxHeight) maxWidth else maxHeight
        Image(
            painter = painterResource(R.drawable.splash_hero_flute),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = base * 0.08f)
                .width(base * 1.6f),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.splash_hero_feather),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = base * -0.21f, y = base * -0.11f)
                .size(width = base * 0.93f, height = base * 1.33f)
                .graphicsLayer {
                    rotationZ = 15f
                    transformOrigin = TransformOrigin(0.22f, 0.86f)
                },
            colorFilter = ColorFilter.tint(
                Color.Black.copy(alpha = 0.28f),
                blendMode = BlendMode.SrcIn,
            ),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(R.drawable.splash_hero_feather),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = base * -0.26f, y = base * -0.17f)
                .size(width = base * 0.93f, height = base * 1.33f)
                .graphicsLayer {
                    rotationZ = 15f
                    transformOrigin = TransformOrigin(0.22f, 0.86f)
                },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun SplashCornerFeatherParticleStreams(
    modifier: Modifier = Modifier,
    seed: Int = 20260515,
    particleCount: Int = 62,
) {
    Box(modifier)
}

@Composable
fun SplashLogoParticleStream(
    modifier: Modifier = Modifier,
    seed: Int = 20260514,
    particleCount: Int = 96,
    sourceXFraction: Float = 0.43f,
    sourceYFraction: Float = 0.30f,
) {
    val phase by rememberSharedLoopPhase(durationMillis = 20_000, offsetMillis = seed.toLong())
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val source = Offset(w * sourceXFraction, h * sourceYFraction)
        val rnd = Random(seed.toLong())
        repeat(particleCount) { index ->
            val stagger = index / particleCount.toFloat()
            val t = (phase + stagger) % 1f
            val baseAngle = rnd.nextFloat() * 2f * PI.toFloat()
            val breath = sin((phase * 1.4f + stagger).toDouble() * PI * 2.0).toFloat()
            val angle = baseAngle + breath * (0.06f + rnd.nextFloat() * 0.08f)
            val maxRadius = w * (0.18f + rnd.nextFloat() * 0.42f)
            val drift = t.pow(0.76f)
            val currentRadius = 10f + drift * maxRadius
            val x = source.x + cos(angle) * currentRadius + breath * w * 0.016f
            val y = source.y + sin(angle) * currentRadius + drift * h * (0.004f + rnd.nextFloat() * 0.035f)
            val fadeIn = kotlin.math.min(1f, t * 5f)
            val fadeOut = (1f - drift).pow(0.48f)
            val bigSpark = index % 19 == 0
            val radius = if (bigSpark) 2.0f + rnd.nextFloat() * 2.8f else 0.7f + rnd.nextFloat() * 1.8f
            val alpha = (if (bigSpark) 0.58f else 0.22f + rnd.nextFloat() * 0.28f) * fadeIn * fadeOut
            if (x >= -radius && x <= w + radius && y >= -radius && y <= h + radius) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SoftGold.copy(alpha = alpha),
                            SacredGold.copy(alpha = alpha * 0.48f),
                            Color.Transparent,
                        ),
                        center = Offset(x, y),
                        radius = radius * if (bigSpark) 6.0f else 4.4f,
                    ),
                    radius = radius * if (bigSpark) 6.0f else 4.4f,
                    center = Offset(x, y),
                )
                drawCircle(
                    color = SoftGold.copy(alpha = alpha * 0.95f),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
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

@Composable
fun SplashOrnamentalDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centerY = size.height * 0.52f
            val gap = 34.dp.toPx()
            val lineLength = 112.dp.toPx()
            val stroke = 2.dp.toPx()
            drawLine(
                color = SacredGold.copy(alpha = 0.96f),
                start = Offset(size.width / 2f - gap - lineLength, centerY),
                end = Offset(size.width / 2f - gap, centerY),
                strokeWidth = stroke,
            )
            drawLine(
                color = SacredGold.copy(alpha = 0.96f),
                start = Offset(size.width / 2f + gap, centerY),
                end = Offset(size.width / 2f + gap + lineLength, centerY),
                strokeWidth = stroke,
            )
        }
        Image(
            painter = painterResource(R.drawable.splash_lotus_icon),
            contentDescription = null,
            modifier = Modifier.size(34.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun SplashLotusTitleRow(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.splash_lotus_icon),
        contentDescription = null,
        modifier = modifier.size(46.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun SplashBeginButtonWithFiligree(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth(0.82f)
            .height(74.dp)
            .widthIn(max = 430.dp),
        contentAlignment = Alignment.Center,
    ) {
        SacredButton(
            onClick = onClick,
            minHeight = 74.dp,
        ) {
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
            .fillMaxWidth(0.58f)
            .height(26.dp),
        alpha = 0.38f,
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
