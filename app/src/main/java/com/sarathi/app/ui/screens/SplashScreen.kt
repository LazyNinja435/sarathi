package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarathi.app.ui.components.PeacockEmblemStyle
import com.sarathi.app.ui.components.PeacockFeatherEmblem
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SplashBeginButtonWithFiligree
import com.sarathi.app.ui.components.SplashCornerFeatherParticleStreams
import com.sarathi.app.ui.components.SplashFooterLotus
import com.sarathi.app.ui.components.SplashLotusTitleRow
import com.sarathi.app.ui.components.SplashLogoParticleStream
import com.sarathi.app.ui.components.SplashOrnamentalDivider
import com.sarathi.app.ui.components.SplashScreenAtmosphere
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun SplashScreen(
    onBegin: () -> Unit,
) {
    SacredBackground(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            SplashScreenAtmosphere(Modifier.fillMaxSize())
            SplashLogoParticleStream(Modifier.fillMaxSize())
            SplashCornerFeatherParticleStreams(Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 30.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    SplashLotusTitleRow()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sarathi",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 28.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = SacredGold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(40.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(292.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PeacockFeatherEmblem(
                            size = 286.dp,
                            style = PeacockEmblemStyle.Hero,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "When the heart grows quiet,\nthe charioteer speaks.",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 30.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = SoftGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 420.dp),
                    )
                    Spacer(Modifier.height(26.dp))
                    SplashOrnamentalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Welcome, dear one.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 22.sp,
                            letterSpacing = 0.25.sp,
                        ),
                        color = SoftGold.copy(alpha = 0.94f),
                        textAlign = TextAlign.Center,
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SplashBeginButtonWithFiligree(onClick = onBegin)
                    Spacer(Modifier.height(10.dp))
                    SplashFooterLotus()
                }
            }
        }
    }
}
