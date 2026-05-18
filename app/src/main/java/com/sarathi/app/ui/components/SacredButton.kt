package com.sarathi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sarathi.app.R
import com.sarathi.app.ui.theme.Ink
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
    fillMaxWidth: Boolean = true,
    minHeight: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val buttonModifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier
    when (style) {
        SacredButtonStyle.GoldSolid -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier.height(minHeight),
                enabled = enabled,
                shape = shape,
                border = null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Ink,
                    disabledContainerColor = Color.Transparent.copy(alpha = 0.4f),
                    disabledContentColor = Ink.copy(alpha = 0.4f),
                ),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                        .height(minHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.sacred_button_plate),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    Row(
                        modifier = Modifier.padding(contentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        content()
                    }
                }
            }
        }
        SacredButtonStyle.GoldOutline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier.heightIn(min = minHeight),
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
        textAlign = TextAlign.Center,
    )
}
