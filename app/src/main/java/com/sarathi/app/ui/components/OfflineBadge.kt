package com.sarathi.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(1.dp, SacredGold, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = SacredGold,
        )
        Column {
            Text(
                text = "Offline Mode",
                style = MaterialTheme.typography.labelLarge,
                color = SoftGold,
            )
            Text(
                text = "No internet needed",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftGold.copy(alpha = 0.85f),
            )
        }
    }
}
