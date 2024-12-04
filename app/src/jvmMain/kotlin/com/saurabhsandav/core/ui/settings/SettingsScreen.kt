package com.saurabhsandav.core.ui.settings

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.saurabhsandav.core.LocalScreensModule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.*
import com.saurabhsandav.core.ui.settings.model.WebViewBackend
import com.saurabhsandav.core.ui.theme.dimens
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile

@Composable
internal fun SettingsWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val screensModule = LocalScreensModule.current
    val presenter = remember { screensModule.settingsModule(scope).presenter() }
    val state by presenter.state.collectAsState()

    AppWindow(
        title = "Settings",
        onCloseRequest = onCloseRequest,
    ) {

        SettingsScreen(
            darkModeEnabled = state.darkModeEnabled,
            onDarkThemeEnabledChange = { state.eventSink(ChangeDarkModeEnabled(it)) },
            landingScreen = state.landingScreen,
            onLandingScreenChange = { state.eventSink(ChangeLandingScreen(it)) },
            densityFraction = state.densityFraction,
            onDensityFractionChange = { state.eventSink(ChangeDensityFraction(it)) },
            defaultTimeframe = state.defaultTimeframe,
            onDefaultTimeframeChange = { state.eventSink(ChangeDefaultTimeframe(it)) },
            webViewBackend = state.webViewBackend,
            onWebViewBackendChange = { state.eventSink(ChangeWebViewBackend(it)) },
            backupProgress = state.backupProgress,
            onBackup = { toDirPath -> state.eventSink(Backup(toDirPath)) },
            onRestore = { archivePath -> state.eventSink(Restore(archivePath)) },
        )
    }
}

@Composable
internal fun SettingsScreen(
    darkModeEnabled: Boolean,
    onDarkThemeEnabledChange: (Boolean) -> Unit,
    landingScreen: LandingScreen,
    onLandingScreenChange: (LandingScreen) -> Unit,
    densityFraction: Float,
    onDensityFractionChange: (Float) -> Unit,
    defaultTimeframe: Timeframe,
    onDefaultTimeframeChange: (Timeframe) -> Unit,
    webViewBackend: WebViewBackend,
    onWebViewBackendChange: (WebViewBackend) -> Unit,
    backupProgress: String?,
    onBackup: (toDirPath: String) -> Unit,
    onRestore: (archivePath: String) -> Unit,
) {

    Box {

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {

            DarkModePreference(
                darkModeEnabled = darkModeEnabled,
                onDarkThemeEnabledChange = onDarkThemeEnabledChange,
            )

            LandingScreenPreference(
                items = remember { enumValues<LandingScreen>().toList() },
                selectedItem = landingScreen,
                onLandingScreenChange = onLandingScreenChange,
            )

            DensityPreference(
                densityFraction = densityFraction,
                onDensityFractionChange = onDensityFractionChange,
            )

            DefaultTimeframePreference(
                items = remember { enumValues<Timeframe>().toList() },
                selectedItem = defaultTimeframe,
                onDefaultTimeframeChange = onDefaultTimeframeChange,
            )

            WebViewBackendPreference(
                items = remember { enumValues<WebViewBackend>().toList() },
                selectedItem = webViewBackend,
                onWebViewBackendChange = onWebViewBackendChange,
            )

            BackupPreference(
                backupProgress = backupProgress,
                onBackup = onBackup,
                onRestore = onRestore,
            )
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
private fun Preference(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) {

    ListItem(
        headlineContent = headlineContent,
        modifier = modifier.padding(MaterialTheme.dimens.listItemPadding),
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = colors,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    )

    HorizontalDivider()
}

@Composable
private fun DarkModePreference(
    darkModeEnabled: Boolean,
    onDarkThemeEnabledChange: (Boolean) -> Unit,
) {

    Preference(
        headlineContent = { Text("Dark Mode") },
        trailingContent = {

            Switch(
                checked = darkModeEnabled,
                onCheckedChange = onDarkThemeEnabledChange,
            )
        },
    )
}

@Composable
private fun LandingScreenPreference(
    items: List<LandingScreen>,
    selectedItem: LandingScreen,
    onLandingScreenChange: (LandingScreen) -> Unit,
) {

    Preference(
        headlineContent = { Text("Landing Screen") },
        trailingContent = {

            OutlinedListSelectionField(
                items = items,
                itemText = { it.title },
                selection = selectedItem,
                onSelection = onLandingScreenChange,
            )
        },
    )
}

@Composable
private fun DensityPreference(
    densityFraction: Float,
    onDensityFractionChange: (Float) -> Unit,
) {

    Preference(
        headlineContent = {

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Density",
                )

                var currentDensityFraction by state(densityFraction) { densityFraction }

                Slider(
                    modifier = Modifier.weight(1F),
                    value = currentDensityFraction,
                    onValueChange = { currentDensityFraction = it },
                    valueRange = 0.5F..1.5F,
                    steps = 10,
                    onValueChangeFinished = {
                        onDensityFractionChange(currentDensityFraction)
                    },
                )
            }
        },
    )
}

@Composable
private fun DefaultTimeframePreference(
    items: List<Timeframe>,
    selectedItem: Timeframe,
    onDefaultTimeframeChange: (Timeframe) -> Unit,
) {

    Preference(
        headlineContent = { Text("Timeframe") },
        trailingContent = {

            OutlinedListSelectionField(
                items = items,
                itemText = { it.toLabel() },
                selection = selectedItem,
                onSelection = onDefaultTimeframeChange,
            )
        },
    )
}

@Composable
private fun WebViewBackendPreference(
    items: List<WebViewBackend>,
    selectedItem: WebViewBackend,
    onWebViewBackendChange: (WebViewBackend) -> Unit,
) {

    Preference(
        headlineContent = { Text("WebView Backend") },
        trailingContent = {

            OutlinedListSelectionField(
                items = items,
                itemText = { it.name },
                selection = selectedItem,
                onSelection = { if (selectedItem != it) onWebViewBackendChange(it) },
            )
        },
    )
}

@Composable
internal fun BackupPreference(
    backupProgress: String?,
    onBackup: (toDirPath: String) -> Unit,
    onRestore: (archivePath: String) -> Unit,
) {

    Preference(
        headlineContent = { Text("Backup") },
        trailingContent = {

            Row(
                modifier = Modifier.wrapContentSize(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                var showDirSelector by state { false }

                Button(
                    onClick = { showDirSelector = true },
                    content = { Text("Backup") },
                )

                LaunchedEffect(showDirSelector) {

                    if (!showDirSelector) return@LaunchedEffect

                    val dir = FileKit.pickDirectory("Backup to")?.path

                    if (dir != null) onBackup(dir)

                    showDirSelector = false
                }

                var showFileSelector by state { false }
                var file by state<String?> { null }

                Button(
                    onClick = { showFileSelector = true },
                    content = { Text("Restore") },
                )

                LaunchedEffect(showFileSelector) {

                    if (!showFileSelector) return@LaunchedEffect

                    file = FileKit.pickFile(
                        type = PickerType.File(listOf("zip")),
                        title = "Restore from",
                    )?.path

                    showFileSelector = false
                }

                @Suppress("LocalVariableName")
                val file_ = file
                if (file_ != null) {

                    ConfirmationDialog(
                        text = "Are you sure you want to restore this backup? (App will restart)",
                        onDismiss = { file = null },
                        onConfirm = {
                            onRestore(file_)
                            file = null
                        },
                    )
                }
            }
        }
    )

    if (backupProgress != null) {

        Dialog(onDismissRequest = {}) {

            Card(
                shape = RoundedCornerShape(16.dp),
            ) {

                Row(
                    modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
                ) {

                    Text(backupProgress)

                    CircularProgressIndicator()
                }
            }
        }
    }
}
