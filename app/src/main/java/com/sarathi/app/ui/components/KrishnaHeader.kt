package com.sarathi.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun KrishnaHeader(
    modifier: Modifier = Modifier,
    showEmblem: Boolean = true,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Sri Krishna",
            style = MaterialTheme.typography.headlineLarge,
            color = SacredGold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your charioteer within",
            style = MaterialTheme.typography.titleMedium,
            color = SoftGold,
            textAlign = TextAlign.Center,
        )
        if (showEmblem) {
            Spacer(Modifier.height(12.dp))
            SplashCenterLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
            )
        }
    }
}
