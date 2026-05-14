package com.sarathi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.Parchment
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

enum class SacredButtonStyle {
    GoldSolid,
    GoldOutline,
}

@Composable
fun SacredButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SacredButtonStyle = SacredButtonStyle.GoldSolid,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    when (style) {
        SacredButtonStyle.GoldSolid -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                enabled = enabled,
                shape = shape,
                border = BorderStroke(1.5.dp, SacredGold),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Ink,
                    disabledContainerColor = Color.Transparent.copy(alpha = 0.4f),
                    disabledContentColor = Ink.copy(alpha = 0.4f),
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(
                            brush = Brush.verticalGradient(listOf(Parchment, SoftGold)),
                            shape = shape,
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    content()
                }
            }
        }
        SacredButtonStyle.GoldOutline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                enabled = enabled,
                shape = shape,
                border = BorderStroke(1.5.dp, SacredGold),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SoftGold,
                ),
                contentPadding = contentPadding,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SacredButtonLabel(text: String, inkOnParchment: Boolean = true) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (inkOnParchment) Ink else SoftGold,
    )
}
