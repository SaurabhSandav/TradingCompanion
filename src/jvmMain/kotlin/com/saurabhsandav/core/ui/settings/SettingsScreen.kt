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
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun SettingsWindow(
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { SettingsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(
        placement = WindowPlacement.Maximized,
    )

    AppWindow(
        state = windowState,
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
        )
    }
}

@Composable
internal fun SettingsScreen(
    darkModeEnabled: Boolean,
    onDarkThemeEnabledChange: (Boolean) -> Unit,
    landingScreen: String,
    onLandingScreenChange: (String) -> Unit,
    densityFraction: Float,
    onDensityFractionChange: (Float) -> Unit,
) {

    Box {

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.verticalScroll(scrollState).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            DarkModePreference(
                darkModeEnabled = darkModeEnabled,
                onDarkThemeEnabledChange = onDarkThemeEnabledChange,
            )

            Divider()

            LandingScreenPreference(
                items = remember { enumValues<LandingScreen>().map { it.title }.toImmutableList() },
                selectedItem = landingScreen,
                onLandingScreenChange = onLandingScreenChange,
            )

            Divider()

            DensityPreference(
                densityFraction = densityFraction,
                onDensityFractionChange = onDensityFractionChange,
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
    items: ImmutableList<String>,
    selectedItem: String,
    onLandingScreenChange: (String) -> Unit,
) {

    ListItem(
        headlineContent = { Text("Landing Screen") },
        trailingContent = {

            ListSelectionField(
                items = items,
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
