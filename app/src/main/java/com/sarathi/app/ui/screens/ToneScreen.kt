package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Spa
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sarathi.app.model.GuidanceTone
import com.sarathi.app.ui.components.ChoiceChipCard
import com.sarathi.app.ui.components.KrishnaHeader
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold

@Composable
fun ToneScreen(
    onContinue: (GuidanceTone) -> Unit,
) {
    var selected by remember { mutableStateOf(GuidanceTone.Gentle) }

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KrishnaHeader()
            Text(
                text = "How shall I guide you, dear one?",
                style = MaterialTheme.typography.titleLarge,
                color = SoftGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RowPair(
                    first = {
                        ChoiceChipCard(
                            title = GuidanceTone.Gentle.label,
                            description = GuidanceTone.Gentle.blurb,
                            selected = selected == GuidanceTone.Gentle,
                            onClick = { selected = GuidanceTone.Gentle },
                            leading = { Icon(Icons.Outlined.Spa, null, tint = SacredGold) },
                        )
                    },
                    second = {
                        ChoiceChipCard(
                            title = GuidanceTone.Direct.label,
                            description = GuidanceTone.Direct.blurb,
                            selected = selected == GuidanceTone.Direct,
                            onClick = { selected = GuidanceTone.Direct },
                            leading = { Icon(Icons.Outlined.Bolt, null, tint = SacredGold) },
                        )
                    },
                )
                RowPair(
                    first = {
                        ChoiceChipCard(
                            title = GuidanceTone.Poetic.label,
                            description = GuidanceTone.Poetic.blurb,
                            selected = selected == GuidanceTone.Poetic,
                            onClick = { selected = GuidanceTone.Poetic },
                            leading = { Icon(Icons.Outlined.AutoAwesome, null, tint = SacredGold) },
                        )
                    },
                    second = {
                        ChoiceChipCard(
                            title = GuidanceTone.Scriptural.label,
                            description = GuidanceTone.Scriptural.blurb,
                            selected = selected == GuidanceTone.Scriptural,
                            onClick = { selected = GuidanceTone.Scriptural },
                            leading = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = SacredGold) },
                        )
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            SacredButton(
                onClick = { onContinue(selected) },
                modifier = Modifier.width(280.dp),
                fillMaxWidth = false,
            ) {
                SacredButtonLabel("Continue")
            }
        }
    }
}

@Composable
private fun RowPair(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) { first() }
        Box(modifier = Modifier.weight(1f)) { second() }
    }
}
