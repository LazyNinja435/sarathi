package com.sarathi.app.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.ModelStatus
import com.sarathi.app.ui.components.SacredBackground
import com.sarathi.app.ui.components.SacredButton
import com.sarathi.app.ui.components.SacredButtonLabel
import com.sarathi.app.ui.components.SacredButtonStyle
import com.sarathi.app.ui.components.SacredCard
import com.sarathi.app.ui.components.SacredCardVariant
import com.sarathi.app.ui.theme.Ink
import com.sarathi.app.ui.theme.SacredGold
import com.sarathi.app.ui.theme.SoftGold
import com.sarathi.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        settingsViewModel.refreshModelStatus()
    }
    val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val status by settingsViewModel.modelStatus.collectAsStateWithLifecycle()
    var showHelp by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = SacredGold)
                }
                Text("Settings", style = MaterialTheme.typography.headlineSmall, color = SacredGold)
            }
            Text(
                text = "Sarathi",
                style = MaterialTheme.typography.titleLarge,
                color = SoftGold,
            )
            Text(
                text = "Offline companion — no internet required for mock mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftGold.copy(alpha = 0.9f),
            )
            SacredCard(variant = SacredCardVariant.Indigo) {
                Text("Model status", style = MaterialTheme.typography.titleMedium, color = SacredGold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (status) {
                        is ModelStatus.Installed -> "Installed: ${(status as ModelStatus.Installed).path}"
                        ModelStatus.Missing -> "Missing — mock responses are active unless you force mock mode."
                        ModelStatus.Loading -> "Loading…"
                        is ModelStatus.Error -> "Error: ${(status as ModelStatus.Error).message}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "The app works in mock mode until a compatible Gemma `.task` file is placed in the models folder (see MODEL_SETUP.md).",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftGold.copy(alpha = 0.85f),
                )
            }
            Text("Expected locations", style = MaterialTheme.typography.titleMedium, color = SacredGold)
            SacredCard(variant = SacredCardVariant.Parchment) {
                ModelManager.expectedPathHints(ctx).forEach { path ->
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Use mock mode", color = SoftGold, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = prefs.useMockMode,
                    onCheckedChange = { settingsViewModel.setUseMockMode(it) },
                )
            }
            SacredButton(onClick = { settingsViewModel.refreshModelStatus() }) {
                SacredButtonLabel("Check model")
            }
            SacredButton(
                onClick = { showHelp = !showHelp },
                style = SacredButtonStyle.GoldOutline,
            ) {
                SacredButtonLabel(if (showHelp) "Hide setup instructions" else "Open setup instructions", inkOnParchment = false)
            }
            if (showHelp) {
                SacredCard(variant = SacredCardVariant.Parchment) {
                    Text(
                        text = "1) Obtain a compatible Gemma `.task` bundle from Google / MediaPipe documentation (terms may apply).\n\n" +
                            "2) Copy the file to your device at either:\n" +
                            "   • Android/data/<package>/files/models/gemma.task (via Device Explorer), or\n" +
                            "   • /sdcard/Download/sarathi/gemma.task (if accessible on your device).\n\n" +
                            "3) Tap \"Check model\". If installed, Sarathi will use MediaPipe for replies.\n\n" +
                            "TODO: optional in-app downloader for approved bundles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink,
                    )
                }
            }
        }
    }
}
