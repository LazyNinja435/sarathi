package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.components.PeacockFeatherEmblem
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.components.SacredButtonStyle
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun SplashScreen(
    onBegin: () -> Unit,
) {
    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PeacockFeatherEmblem()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Sarathi",
                    style = MaterialTheme.typography.displayLarge,
                    color = SacredGold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "When the heart grows quiet,\nthe charioteer speaks.",
                    style = MaterialTheme.typography.titleLarge,
                    color = SoftGold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Welcome, dear one.",
                    style = MaterialTheme.typography.titleMedium,
                    color = SoftGold,
                    textAlign = TextAlign.Center,
                )
            }
            SacredButton(onClick = onBegin) {
                SacredButtonLabel("Begin")
            }
        }
    }
}
