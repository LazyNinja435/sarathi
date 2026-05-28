package com.sarathi.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sarathi.app.BuildConfig
import com.sarathi.app.SarathiApp
import com.sarathi.app.llm.ModelManager
import com.sarathi.app.model.OnDeviceWisdomStatus
import com.sarathi.app.modeldownload.ModelDownloadAction
import com.sarathi.app.modeldownload.ModelDownloadUiState
import com.sarathi.app.modeldownload.ModelInstallViewModel
import com.sarathi.app.update.ApkInstaller
import com.sarathi.app.update.ManifestCache
import com.sarathi.app.update.ReleaseManifest
import com.sarathi.app.update.UpdateUiState
import com.sarathi.app.update.UpdateViewModel
import com.sarathi.app.model.LlmModelFileKind
import com.sarathi.app.model.LlmRuntimeKind
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
    onResetOnboarding: () -> Unit,
) {
    LaunchedEffect(Unit) {
        settingsViewModel.refreshModelStatus()
    }
    val prefs by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val status by settingsViewModel.modelStatus.collectAsStateWithLifecycle()
    val diag by settingsViewModel.llmDiagnostics.collectAsStateWithLifecycle()
    val ragReady by settingsViewModel.ragReady.collectAsStateWithLifecycle()
    LaunchedEffect(prefs.customModelPath) {
        settingsViewModel.refreshModelStatus()
    }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var devOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SarathiApp
    val activity = ctx as? Activity
    val updateVm: UpdateViewModel = viewModel(factory = app.viewModelFactory)
    val modelVm: ModelInstallViewModel = viewModel(factory = app.viewModelFactory)
    val updateUi by updateVm.ui.collectAsStateWithLifecycle()
    val modelUi by modelVm.ui.collectAsStateWithLifecycle()
    val updateManifestUrl by updateVm.lastManifestUrl.collectAsStateWithLifecycle()
    val apkSha by updateVm.lastDownloadedApkSha.collectAsStateWithLifecycle()
    val updateLastError by updateVm.lastError.collectAsStateWithLifecycle()
    val modelLastError by modelVm.lastError.collectAsStateWithLifecycle()
    val manifestRev by ManifestCache.revision.collectAsStateWithLifecycle()
    val wisdom = remember(prefs.customModelPath, manifestRev) {
        OnDeviceWisdomStatus.evaluate(ctx, prefs.customModelPath)
    }
    var showModelDownloadConfirm by remember { mutableStateOf(false) }
    var showModelUpdateConfirm by remember { mutableStateOf(false) }
    var showModelVerifyConfirm by remember { mutableStateOf(false) }
    var dismissedUpdate by remember { mutableStateOf(false) }
    LaunchedEffect(modelUi) {
        if (modelUi is ModelDownloadUiState.Installed) {
            settingsViewModel.refreshModelStatus()
            modelVm.acknowledgeInstalled()
        }
    }

    SacredBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                text = "A calm companion for the journey within — crafted for privacy and quiet reflection.",
                style = MaterialTheme.typography.bodyMedium,
                color = SoftGold.copy(alpha = 0.88f),
            )

            SacredCard(variant = SacredCardVariant.Indigo) {
                Text("Guidance & privacy", style = MaterialTheme.typography.titleMedium, color = SacredGold)
                Spacer(Modifier.height(10.dp))
                settingRow("Offline mode", "Enabled — your conversations stay on this device.")
                Spacer(Modifier.height(8.dp))
                settingRow(
                    "Guidance engine",
                    friendlyEngineLabel(active = diag.activeRuntime),
                )
                Spacer(Modifier.height(8.dp))
                settingRow("Model status", friendlyModelStatus(status))
                Spacer(Modifier.height(8.dp))
                settingRow("Tone of voice", prefs.selectedTone.label)
                Spacer(Modifier.height(8.dp))
                settingRow("Saved guidance memory", "${prefs.userMemory.savedUserNotes.size} saved")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SacredButton(
                        onClick = { settingsViewModel.refreshModelStatus() },
                        fillMaxWidth = false,
                    ) {
                        SacredButtonLabel("Check model")
                    }
                    SacredButton(
                        onClick = { settingsViewModel.clearUserMemory() },
                        enabled = prefs.userMemory.savedUserNotes.isNotEmpty(),
                        style = SacredButtonStyle.GoldOutline,
                        fillMaxWidth = false,
                    ) {
                        SacredButtonLabel("Clear memory", inkOnParchment = false)
                    }
                }
            }

            SacredCard(variant = SacredCardVariant.Indigo) {
                Text("Update Sarathi", style = MaterialTheme.typography.titleMedium, color = SacredGold)
                Spacer(Modifier.height(10.dp))
                settingRow("App version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Spacer(Modifier.height(10.dp))
                SacredButton(onClick = {
                    dismissedUpdate = false
                    updateVm.checkForUpdates()
                }) {
                    SacredButtonLabel("Check for updates")
                }
                Spacer(Modifier.height(10.dp))
                when (val u = updateUi) {
                    UpdateUiState.Idle -> { }
                    UpdateUiState.Checking -> Text("Checking…", color = SoftGold, style = MaterialTheme.typography.bodyMedium)
                    is UpdateUiState.UpToDate -> Text("Sarathi is up to date.", color = SoftGold, style = MaterialTheme.typography.bodyMedium)
                    is UpdateUiState.UpdateAvailable -> {
                        if (dismissedUpdate) {
                            Text(
                                "An update is still available when you are ready.",
                                color = SoftGold,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text("App update available", color = SoftGold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            when (u.manifest.release.releaseType) {
                                ReleaseManifest.ReleaseType.APP_ONLY -> {
                                    Text(
                                        "This update improves Sarathi. Your offline wisdom model will remain installed.",
                                        color = SoftGold,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                ReleaseManifest.ReleaseType.FULL_MODEL,
                                ReleaseManifest.ReleaseType.MODEL_ONLY,
                                -> {
                                    if (u.manifest.app?.requiresModelUpdate == true) {
                                        Text(
                                            "This version requires a newer offline model for on-device wisdom.",
                                            color = SoftGold,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    } else {
                                        Text(
                                            "A newer offline model is also available, but you can update the app first.",
                                            color = SoftGold,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Version ${u.versionName}", color = SoftGold, style = MaterialTheme.typography.bodyMedium)
                            Text("APK size: ${formatBytes(u.apkSizeBytes)}", color = SoftGold, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SacredButton(onClick = { updateVm.downloadUpdate() }) {
                                    SacredButtonLabel(
                                        when (u.manifest.release.releaseType) {
                                            ReleaseManifest.ReleaseType.APP_ONLY -> "Download app update"
                                            ReleaseManifest.ReleaseType.FULL_MODEL,
                                            ReleaseManifest.ReleaseType.MODEL_ONLY,
                                            ->
                                                if (u.manifest.app?.requiresModelUpdate == true) "Update app" else "Download app update"
                                        },
                                    )
                                }
                                SacredButton(
                                    onClick = { dismissedUpdate = true },
                                    style = SacredButtonStyle.GoldOutline,
                                ) {
                                    SacredButtonLabel(
                                        when (u.manifest.release.releaseType) {
                                            ReleaseManifest.ReleaseType.APP_ONLY -> "Later"
                                            ReleaseManifest.ReleaseType.FULL_MODEL,
                                            ReleaseManifest.ReleaseType.MODEL_ONLY,
                                            ->
                                                if (u.manifest.app?.requiresModelUpdate == true) {
                                                    "Download model after update"
                                                } else {
                                                    "Download model later"
                                                }
                                        },
                                        inkOnParchment = false,
                                    )
                                }
                            }
                        }
                    }
                    UpdateUiState.Downloading -> Text("Downloading update…", color = SoftGold, style = MaterialTheme.typography.bodyMedium)
                    UpdateUiState.ReadyToInstall -> {
                        Text(
                            "Android will ask you to confirm installation.",
                            color = SoftGold,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        SacredButton(
                            onClick = {
                                val act = activity
                                if (act == null) return@SacredButton
                                if (!ApkInstaller.canInstallPackages(act)) {
                                    ApkInstaller.openInstallPermissionSettings(act)
                                } else {
                                    ApkInstaller.installDownloadedApk(act, updateVm.pendingUpdateApkFile())
                                }
                            },
                        ) {
                            SacredButtonLabel("Install update")
                        }
                    }
                    is UpdateUiState.Error -> {
                        Text(u.message, color = SoftGold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        SacredButton(onClick = { updateVm.resetAfterError() }) {
                            SacredButtonLabel("Dismiss", inkOnParchment = false)
                        }
                    }
                }
            }

            SacredCard(variant = SacredCardVariant.Indigo) {
                Text("Offline model", style = MaterialTheme.typography.titleMedium, color = SacredGold)
                Spacer(Modifier.height(8.dp))
                when (wisdom) {
                    OnDeviceWisdomStatus.Missing -> {
                        Text(
                            "Download offline model",
                            color = SacredGold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "On-device wisdom is not installed yet.",
                            color = SoftGold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        SacredButton(onClick = { showModelDownloadConfirm = true }) {
                            SacredButtonLabel("Download offline model")
                        }
                    }
                    OnDeviceWisdomStatus.Ready -> {
                        Text(
                            "On-device wisdom: Ready",
                            color = SacredGold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Your offline model is already installed. Gemma stays in app-private storage.",
                            color = SoftGold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        SacredButton(
                            onClick = { showModelVerifyConfirm = true },
                            style = SacredButtonStyle.GoldOutline,
                        ) {
                            SacredButtonLabel("Verify model", inkOnParchment = false)
                        }
                    }
                    is OnDeviceWisdomStatus.OptionalModelUpdate -> {
                        Text(
                            "New offline model available",
                            color = SacredGold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "Size: ${wisdom.approxSizeLabel}. The app will ask before downloading.",
                            color = SoftGold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        SacredButton(onClick = { showModelUpdateConfirm = true }) {
                            SacredButtonLabel("Download model update")
                        }
                    }
                    OnDeviceWisdomStatus.ModelUpdateRequired -> {
                        Text(
                            "Model update required for on-device wisdom",
                            color = SacredGold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "This Sarathi version needs a newer offline model for on-device wisdom.",
                            color = SoftGold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        SacredButton(onClick = { showModelUpdateConfirm = true }) {
                            SacredButtonLabel("Download model update")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                when (val m = modelUi) {
                    ModelDownloadUiState.Idle -> { }
                    ModelDownloadUiState.FetchingManifest -> Text("Preparing…", color = SoftGold)
                    ModelDownloadUiState.Downloading -> Text("Downloading…", color = SoftGold)
                    is ModelDownloadUiState.Progress -> {
                        Text(m.label, color = SoftGold, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${formatBytes(m.downloadedBytes)} / ${formatBytes(m.totalBytes)}",
                            color = SoftGold,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    ModelDownloadUiState.Verifying -> Text("Verifying model (this can take a while)…", color = SoftGold)
                    ModelDownloadUiState.Installed -> Text("Model install or verification finished.", color = SoftGold)
                    is ModelDownloadUiState.Error -> {
                        Text(m.message, color = SoftGold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        SacredButton(onClick = { modelVm.resetAfterError() }) {
                            SacredButtonLabel("Dismiss", inkOnParchment = false)
                        }
                    }
                }
            }

            SacredButton(
                onClick = { confirmReset = true },
                style = SacredButtonStyle.GoldOutline,
            ) {
                SacredButtonLabel("Reset onboarding journey", inkOnParchment = false)
            }

            SacredButton(
                onClick = { showAbout = true },
                style = SacredButtonStyle.GoldOutline,
            ) {
                SacredButtonLabel("About Sarathi", inkOnParchment = false)
            }

            SacredButton(
                onClick = { devOpen = !devOpen },
                style = SacredButtonStyle.GoldOutline,
            ) {
                SacredButtonLabel(if (devOpen) "Hide developer diagnostics" else "Developer diagnostics", inkOnParchment = false)
            }

            if (devOpen) {
                SacredCard(variant = SacredCardVariant.Parchment) {
                    Text("Developer diagnostics", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    monoLine("Active runtime", runtimeLabel(diag.activeRuntime))
                    monoLine("Model path", diag.selectedPath ?: "(none)")
                    monoLine("Model file type", modelFileLabel(diag.modelFileKind))
                    monoLine("RAG database", if (ragReady) "Ready" else "Missing or not loaded")
                    monoLine("Last inference error", settingsViewModel.lastInferenceError() ?: "(none)")
                    monoLine("Manifest URL (updates)", updateManifestUrl)
                    monoLine("Downloaded update APK SHA", apkSha ?: "(none)")
                    monoLine("Last update error", updateLastError ?: "(none)")
                    monoLine("Last model download error", modelLastError ?: "(none)")
                    Spacer(Modifier.height(8.dp))
                    SacredButton(onClick = { settingsViewModel.refreshModelStatus() }) {
                        SacredButtonLabel("Refresh diagnostics", inkOnParchment = true)
                    }
                }
                SacredCard(variant = SacredCardVariant.Indigo) {
                    Text("Expected model paths", style = MaterialTheme.typography.titleSmall, color = SacredGold)
                    Spacer(Modifier.height(6.dp))
                    ModelManager.expectedPathHints(ctx).take(8).forEach { path ->
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            SacredButton(
                onClick = { showHelp = !showHelp },
                style = SacredButtonStyle.GoldOutline,
            ) {
                SacredButtonLabel(if (showHelp) "Hide setup instructions" else "Model setup instructions", inkOnParchment = false)
            }
            if (showHelp) {
                SacredCard(variant = SacredCardVariant.Parchment) {
                    Text(
                        text = "1) Obtain a Gemma LiteRT-LM .litertlm (recommended) or a MediaPipe-compatible .task bundle from official channels (terms may apply).\n\n" +
                            "2) Place the file in app-private storage (recommended), for example files/models/ with the expected filename. Public Download folders may be blocked on modern Android.\n\n" +
                            "3) Tap \"Check model\". When a .litertlm is present and practice mode is off, Sarathi uses LiteRT-LM; otherwise it tries MediaPipe for .task files.\n\n" +
                            "Optional: use the Sarathi Pixel Bundle install script for a repeatable private copy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink,
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            confirmButton = {
                AlertDialogActionButton(
                    text = "Reset",
                    onClick = {
                        confirmReset = false
                        settingsViewModel.resetOnboarding(onResetOnboarding)
                    },
                )
            },
            dismissButton = {
                AlertDialogActionButton(
                    text = "Cancel",
                    onClick = { confirmReset = false },
                    outline = true,
                )
            },
            title = { Text("Reset onboarding?") },
            text = { Text("You will return to the welcome path. Your model files are not removed.") },
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                AlertDialogActionButton(
                    text = "Close",
                    onClick = { showAbout = false },
                )
            },
            title = { Text("About Sarathi") },
            text = {
                Text(
                    "Sarathi is a Krishna-inspired companion rooted in the spirit of the Bhagavad Gita — " +
                        "meant to steady the heart, clarify duty, and speak with warmth. " +
                        "It is not a generic chatbot: it is a charioteer for your inner battlefield.",
                )
            },
        )
    }

    if (showModelDownloadConfirm) {
        AlertDialog(
            onDismissRequest = { showModelDownloadConfirm = false },
            confirmButton = {
                AlertDialogActionButton(
                    text = "Download",
                    onClick = {
                        showModelDownloadConfirm = false
                        modelVm.startDownload(action = ModelDownloadAction.INSTALL_MODEL)
                    },
                )
            },
            dismissButton = {
                AlertDialogActionButton(
                    text = "Cancel",
                    onClick = { showModelDownloadConfirm = false },
                    outline = true,
                )
            },
            title = { Text("Download offline model?") },
            text = {
                Text(
                    "This download is about 2.6 GB. Wi‑Fi is recommended. Keep Sarathi open until it finishes. " +
                        "The model is stored once in app-private storage (not your public Downloads folder).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }

    if (showModelVerifyConfirm) {
        AlertDialog(
            onDismissRequest = { showModelVerifyConfirm = false },
            confirmButton = {
                AlertDialogActionButton(
                    text = "Verify",
                    onClick = {
                        showModelVerifyConfirm = false
                        modelVm.startDownload(action = ModelDownloadAction.VERIFY_MODEL)
                    },
                )
            },
            dismissButton = {
                AlertDialogActionButton(
                    text = "Cancel",
                    onClick = { showModelVerifyConfirm = false },
                    outline = true,
                )
            },
            title = { Text("Verify model?") },
            text = {
                Text(
                    "Sarathi will compute a SHA-256 of your on-device model. This can take several minutes for a large file.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }

    if (showModelUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { showModelUpdateConfirm = false },
            confirmButton = {
                AlertDialogActionButton(
                    text = "Download",
                    onClick = {
                        showModelUpdateConfirm = false
                        modelVm.startDownload(action = ModelDownloadAction.UPDATE_MODEL)
                    },
                )
            },
            dismissButton = {
                AlertDialogActionButton(
                    text = "Cancel",
                    onClick = { showModelUpdateConfirm = false },
                    outline = true,
                )
            },
            title = { Text("Download model update?") },
            text = {
                Text(
                    "This download is about 2.6 GB. Wi‑Fi is recommended. Keep Sarathi open until it finishes. " +
                        "The model stays in app-private storage.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
}

@Composable
private fun settingRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = SacredGold.copy(alpha = 0.9f))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = SoftGold)
    }
}

@Composable
private fun monoLine(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.copy(alpha = 0.65f))
    Text(
        value,
        style = MaterialTheme.typography.bodySmall,
        color = Ink,
        fontFamily = FontFamily.Monospace,
    )
    Spacer(Modifier.height(6.dp))
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) {
        v /= 1024.0
        i++
    }
    return String.format("%.1f %s", v, units[i])
}

private fun friendlyEngineLabel(active: LlmRuntimeKind): String = when (active) {
    LlmRuntimeKind.ServerManagedCloud -> "Sarathi online"
    LlmRuntimeKind.LiteRtLm -> "On-device Gemma"
    LlmRuntimeKind.MediaPipe -> "On-device Gemma (classic engine)"
    else -> "Vaikuntha unreachable"
}

private fun friendlyModelStatus(status: ModelStatus): String = when (status) {
    is ModelStatus.Installed -> "Ready"
    ModelStatus.Missing -> "Not installed"
    ModelStatus.Loading -> "Loading…"
    is ModelStatus.Error -> "Error — ${status.message}"
}

private fun runtimeLabel(kind: LlmRuntimeKind): String = when (kind) {
    LlmRuntimeKind.Mock -> "Unavailable"
    LlmRuntimeKind.ServerManagedCloud -> "Sarathi online"
    LlmRuntimeKind.LiteRtLm -> "LiteRT-LM"
    LlmRuntimeKind.MediaPipe -> "MediaPipe"
}

private fun modelFileLabel(kind: LlmModelFileKind): String = when (kind) {
    LlmModelFileKind.Missing -> "Missing"
    LlmModelFileKind.LiteRtLm -> "LiteRT-LM .litertlm"
    LlmModelFileKind.MediaPipeTask -> "MediaPipe .task"
}

@Composable
private fun AlertDialogActionButton(
    text: String,
    onClick: () -> Unit,
    outline: Boolean = false,
) {
    SacredButton(
        onClick = onClick,
        style = if (outline) SacredButtonStyle.GoldOutline else SacredButtonStyle.GoldSolid,
        fillMaxWidth = false,
        minHeight = 40.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        SacredButtonLabel(text, inkOnParchment = !outline)
    }
}
