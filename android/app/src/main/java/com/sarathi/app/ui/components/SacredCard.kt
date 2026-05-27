package com.sarathi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.theme.IndigoBubble
import com.sarathi.app.ui.theme.Parchment
import com.sarathi.app.ui.theme.SacredGold

enum class SacredCardVariant {
    Parchment,
    Indigo,
}

@Composable
fun SacredCard(
    modifier: Modifier = Modifier,
    variant: SacredCardVariant = SacredCardVariant.Parchment,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val (container, border) = when (variant) {
        SacredCardVariant.Parchment -> Parchment to SacredGold
        SacredCardVariant.Indigo -> IndigoBubble.copy(alpha = 0.92f) to SacredGold
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = container,
        border = BorderStroke(1.5.dp, border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}
