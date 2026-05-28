package com.sarathi.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sarathi.app.model.GuidanceSurface
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun OfflineBadge(
    modifier: Modifier = Modifier,
    surface: GuidanceSurface = GuidanceSurface.OfflineGuidance,
) = OfflineBadgeContent(modifier = modifier, surface = surface)

@Composable
private fun OfflineBadgeContent(
    modifier: Modifier = Modifier,
    surface: GuidanceSurface = GuidanceSurface.OfflineGuidance,
) {
    val (title, subtitle, icon) = when (surface) {
        GuidanceSurface.Practice,
        GuidanceSurface.OfflineGuidance,
        -> Triple(
            "Vaikuntha unreachable",
            "Check connection settings",
            Icons.Outlined.WifiOff,
        )

        GuidanceSurface.ServerManagedCloud -> Triple(
            "Online guidance",
            "Sarathi chooses the best available route",
            Icons.Outlined.Cloud,
        )

        GuidanceSurface.OnDeviceGemma -> Triple(
            "On-device wisdom",
            "Gemma runs privately on this device",
            Icons.Outlined.Psychology,
        )

        GuidanceSurface.OnDeviceMediaPipe -> Triple(
            "On-device wisdom",
            "Guidance runs with the bundled engine",
            Icons.Outlined.Psychology,
        )

        GuidanceSurface.ModelUpdateRequired -> Triple(
            "On-device wisdom",
            "Model update required - open Settings to download the new offline model",
            Icons.Outlined.Psychology,
        )
    }
    Row(
        modifier = modifier
            .border(1.dp, SacredGold, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SacredGold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftGold,
                )
                if (surface == GuidanceSurface.OnDeviceGemma || surface == GuidanceSurface.OnDeviceMediaPipe ||
                    surface == GuidanceSurface.ModelUpdateRequired
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = SacredGold.copy(alpha = 0.55f),
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SoftGold.copy(alpha = 0.82f),
            )
        }
    }
}
