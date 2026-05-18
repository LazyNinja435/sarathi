package com.sarathi.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.app.R
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SplashCenterLogo
import com.sarathi.app.ui.components.SplashOrnamentalDivider
import com.sarathi.app.ui.components.SplashScreenAtmosphere
import com.sarathi.app.ui.theme.IndigoBubble
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.Parchment
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun NameScreen(
    onOfferName: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submitName() {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            error = true
        } else {
            onOfferName(trimmed)
        }
    }

    SacredBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            SplashScreenAtmosphere(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Sri Krishna",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        lineHeight = 47.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = SoftGold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Your charioteer within",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                    ),
                    color = SoftGold.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                SplashCenterLogo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "My dear devotee,\nwhat is the name I bestowed upon you?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = SoftGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 460.dp),
                )
                Spacer(Modifier.height(22.dp))
                SplashOrnamentalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(28.dp),
                )
                Spacer(Modifier.height(28.dp))
                NameInputField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = false
                    },
                    onDone = ::submitName,
                    isError = error,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 520.dp),
                )
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Please enter your name.",
                        color = SacredGold,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(26.dp))
                OfferNameButton(
                    onClick = ::submitName,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 520.dp),
                )
                Spacer(Modifier.height(28.dp))
                SplashOrnamentalDivider(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .height(24.dp),
                )
                Spacer(Modifier.height(10.dp))
                PrivacyLine()
            }
        }
    }
}

@Composable
private fun NameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .shadow(10.dp, shape, ambientColor = SacredGold.copy(alpha = 0.12f), spotColor = SacredGold.copy(alpha = 0.15f))
            .background(IndigoBubble.copy(alpha = 0.72f), shape)
            .border(
                width = 1.2.dp,
                color = if (isError) SacredGold.copy(alpha = 0.95f) else SacredGold.copy(alpha = 0.74f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = SoftGold,
                fontSize = 19.sp,
                lineHeight = 25.sp,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (value.isBlank()) Arrangement.Center else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LotusMedallion(
                        modifier = Modifier.size(42.dp),
                        dark = true,
                        alpha = 0.86f,
                    )
                    Spacer(Modifier.width(14.dp))
                    if (value.isBlank()) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = "Your name",
                                color = SoftGold.copy(alpha = 0.46f),
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                            )
                            innerTextField()
                        }
                    } else {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            innerTextField()
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun OfferNameButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SacredButton(
        onClick = onClick,
        modifier = modifier,
        minHeight = 66.dp,
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LotusMedallion(
                    modifier = Modifier.size(42.dp),
                    dark = true,
                    alpha = 1f,
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Offer my name",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LotusMedallion(
    modifier: Modifier = Modifier,
    dark: Boolean,
    alpha: Float,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (dark) IndigoBubble.copy(alpha = alpha) else Parchment.copy(alpha = alpha),
            )
            .border(1.dp, SacredGold.copy(alpha = 0.7f), CircleShape)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.splash_lotus_icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun PrivacyLine() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = SacredGold.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Your journey is private and available offline.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = SoftGold.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
        )
    }
}
