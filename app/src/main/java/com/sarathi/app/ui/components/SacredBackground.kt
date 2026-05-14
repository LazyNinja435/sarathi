package com.sarathi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sarathi.app.ui.theme.DeepBlue
import com.sarathi.app.ui.theme.MidnightIndigo
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import kotlin.random.Random

@Composable
fun SacredBackground(
    modifier: Modifier = Modifier,
    seed: Int = 42,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlue, MidnightIndigo, DeepBlue, MidnightIndigo),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = Random(seed.toLong())
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SacredGold.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.12f),
                    radius = w * 0.85f,
                ),
                radius = w * 0.9f,
                center = Offset(w * 0.5f, h * 0.1f),
            )
            repeat(48) {
                val x = r.nextFloat() * w
                val y = r.nextFloat() * h
                val a = 0.08f + r.nextFloat() * 0.12f
                drawCircle(
                    color = SoftGold.copy(alpha = a),
                    radius = 1f + r.nextFloat() * 1.8f,
                    center = Offset(x, y),
                )
            }
            drawMandalaCorner(
                topLeft = true,
                size = size.minDimension * 0.42f,
                origin = Offset(0f, 0f),
            )
            drawMandalaCorner(
                topLeft = false,
                size = size.minDimension * 0.42f,
                origin = Offset(w, h),
            )
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMandalaCorner(
    topLeft: Boolean,
    size: Float,
    origin: Offset,
) {
    val stroke = Stroke(width = 1.2f)
    val c = SacredGold.copy(alpha = 0.08f)
    val cx = if (topLeft) origin.x + size * 0.45f else origin.x - size * 0.45f
    val cy = if (topLeft) origin.y + size * 0.45f else origin.y - size * 0.45f
    for (i in 1..6) {
        drawCircle(
            color = c,
            radius = size * 0.08f * i,
            center = Offset(cx, cy),
            style = stroke,
        )
    }
    val spokes = 16
    val r = size * 0.35f
    for (s in 0 until spokes) {
        val ang = (Math.PI * 2 * s / spokes).toFloat()
        val x2 = cx + kotlin.math.cos(ang.toDouble()).toFloat() * r
        val y2 = cy + kotlin.math.sin(ang.toDouble()).toFloat() * r
        drawLine(
            color = c,
            start = Offset(cx, cy),
            end = Offset(x2, y2),
            strokeWidth = 0.8f,
        )
    }
}
