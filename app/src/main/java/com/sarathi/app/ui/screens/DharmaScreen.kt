package com.sarathi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarathi.app.ui.components.KrishnaHeader
import com.sarathi.app.ui.components.OfflineBadge
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.components.SacredButtonStyle
import com.sarathi.app.ui.components.SacredCard
import com.sarathi.app.ui.components.SacredCardVariant
import com.sarathi.app.ui.components.SacredTextField
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import com.sarathi.app.viewmodel.DharmaViewModel

@Composable
fun DharmaScreen(
    dharmaViewModel: DharmaViewModel,
    onReflectWithKrishna: (String) -> Unit,
    onMenu: () -> Unit,
) {
    val saved by dharmaViewModel.note.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    LaunchedEffect(saved) {
        text = saved
    }

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = SacredGold)
                }
                OfflineBadge()
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("My Dharma", style = MaterialTheme.typography.headlineMedium, color = SacredGold)
                Text("A reflection with your charioteer", style = MaterialTheme.typography.titleMedium, color = SoftGold)
            }
            KrishnaHeader(showEmblem = true)
            SacredCard(variant = SacredCardVariant.Parchment) {
                Text(
                    text = "What duty are you avoiding, dear one?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
            }
            SacredTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "Write what you have been postponing…",
                singleLine = false,
                maxLines = 8,
                leading = { Icon(Icons.Outlined.Edit, null, tint = SacredGold) },
            )
            SacredCard(variant = SacredCardVariant.Parchment) {
                Text(
                    text = "Duty often feels heavy not because it is wrong, but because the mind has dressed it in fear.\n\nName it clearly. Face it gently. Then take one sincere step.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SacredButton(onClick = { onReflectWithKrishna(text) }, modifier = Modifier.weight(1f)) {
                    SacredButtonLabel("Reflect with Krishna")
                }
                SacredButton(
                    onClick = { dharmaViewModel.save(text) },
                    modifier = Modifier.weight(1f),
                    style = SacredButtonStyle.GoldOutline,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.Lock, null, tint = SoftGold)
                        Spacer(Modifier.width(8.dp))
                        SacredButtonLabel("Save privately", inkOnParchment = false)
                    }
                }
            }
        }
    }
}
