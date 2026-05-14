package com.sarathi.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarathi.app.model.Verse
import com.sarathi.app.ui.components.OfflineBadge
import com.sarathi.app.ui.components.PeacockFeatherEmblem
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.components.SacredButtonStyle
import com.sarathi.app.ui.components.SacredCard
import com.sarathi.app.ui.components.SacredCardVariant
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import com.sarathi.app.viewmodel.VerseViewModel

@Composable
fun VerseScreen(
    verseViewModel: VerseViewModel,
    onBack: () -> Unit,
    onReflectWithVerse: (Verse) -> Unit,
) {
    val verse by verseViewModel.verse.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SacredGold)
                }
                OfflineBadge()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Verse of the Day", style = MaterialTheme.typography.headlineMedium, color = SacredGold)
                Text("Today's whisper", style = MaterialTheme.typography.titleMedium, color = SoftGold.copy(alpha = 0.9f))
                Spacer(Modifier.height(8.dp))
                PeacockFeatherEmblem()
            }
            Spacer(Modifier.height(16.dp))
            val v = verse
            if (v != null) {
                SacredCard(variant = SacredCardVariant.Parchment) {
                    Text(v.referenceLabel, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = SacredGold.copy(alpha = 0.35f))
                    Spacer(Modifier.height(8.dp))
                    Text(v.translation, style = MaterialTheme.typography.bodyLarge, color = Ink, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                SacredCard(variant = SacredCardVariant.Indigo) {
                    Text(v.reflection, style = MaterialTheme.typography.bodyLarge, color = SoftGold, textAlign = TextAlign.Center)
                }
                if (!v.sourceAttribution.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        v.sourceAttribution,
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGold.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SacredButton(
                        onClick = { onReflectWithVerse(v) },
                        modifier = Modifier.weight(1f),
                    ) {
                        SacredButtonLabel("Reflect")
                    }
                    SacredButton(
                        onClick = {
                            val text = "${v.referenceLabel}\n\n${v.translation}\n\n${v.reflection}"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Share verse"))
                        },
                        modifier = Modifier.weight(1f),
                        style = SacredButtonStyle.GoldOutline,
                    ) {
                        SacredButtonLabel("Share verse", inkOnParchment = false)
                    }
                }
            }
        }
    }
}
