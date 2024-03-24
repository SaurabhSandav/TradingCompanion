package com.saurabhsandav.core.ui.settings

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.LocalAppModule
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

@Composable
internal fun SettingsWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.settingsModule(scope).presenter() }
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
) {

    Box {

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.verticalScroll(scrollState).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            DarkModePreference(
                darkModeEnabled = darkModeEnabled,
                onDarkThemeEnabledChange = onDarkThemeEnabledChange,
            )

            HorizontalDivider()

            LandingScreenPreference(
                items = remember { enumValues<LandingScreen>().toList() },
                selectedItem = landingScreen,
                onLandingScreenChange = onLandingScreenChange,
            )

            HorizontalDivider()

            DensityPreference(
                densityFraction = densityFraction,
                onDensityFractionChange = onDensityFractionChange,
            )

            HorizontalDivider()

            DefaultTimeframePreference(
                items = remember { enumValues<Timeframe>().toList() },
                selectedItem = defaultTimeframe,
                onDefaultTimeframeChange = onDefaultTimeframeChange,
            )

            HorizontalDivider()

            WebViewBackendPreference(
                items = remember { enumValues<WebViewBackend>().toList() },
                selectedItem = webViewBackend,
                onWebViewBackendChange = onWebViewBackendChange,
            )
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState)
        )
    }
}

@Composable
private fun DarkModePreference(
    darkModeEnabled: Boolean,
    onDarkThemeEnabledChange: (Boolean) -> Unit,
) {

    ListItem(
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

    ListItem(
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

    ListItem(
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

    ListItem(
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

    var newBackend by state<WebViewBackend?> { null }

    ListItem(
        headlineContent = { Text("WebView Backend") },
        trailingContent = {

            OutlinedListSelectionField(
                items = items,
                itemText = { it.name },
                selection = selectedItem,
                onSelection = { if (selectedItem != it) newBackend = it },
            )
        },
    )

    if (newBackend != null) {

        ConfirmationDialog(
            text = "Are you sure you want to change the WebView Backend? (App will restart)",
            onDismiss = { newBackend = null },
            onConfirm = {
                onWebViewBackendChange(newBackend!!)
                newBackend = null
            },
        )
    }
}
