package com.sarathi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.theme.DeepBlue
import com.sarathi.app.ui.theme.PeacockGreen
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PeacockFeatherEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier.size(120.dp)) {
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
        val feather = Path().apply {
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
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.14f),
        )
        drawOval(
            color = DeepBlue,
            topLeft = Offset(cx - w * 0.05f, cy + h * 0.01f),
            size = androidx.compose.ui.geometry.Size(w * 0.1f, h * 0.08f),
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
}
