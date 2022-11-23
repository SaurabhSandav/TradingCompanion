package ui.settings

import AppModule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.controls.ListSelectionField
import ui.common.state
import ui.landing.model.LandingScreen
import ui.settings.model.SettingsEvent.*

@Composable
internal fun SettingsScreen(appModule: AppModule) {

    val scope = rememberCoroutineScope()
    val presenter = remember { SettingsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    SettingsScreen(
        darkModeEnabled = state.darkModeEnabled,
        onDarkThemeEnabledChange = { presenter.event(ChangeDarkModeEnabled(it)) },
        landingScreen = state.landingScreen,
        onLandingScreenChange = { presenter.event(ChangeLandingScreen(it)) },
        densityFraction = state.densityFraction,
        onDensityFractionChange = { presenter.event(ChangeDensityFraction(it)) },
    )
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

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        DarkModePreference(
            darkModeEnabled = darkModeEnabled,
            onDarkThemeEnabledChange = onDarkThemeEnabledChange,
        )

        Divider()

        LandingScreenPreference(
            items = remember { LandingScreen.items.map { it.title } },
            selectedItem = landingScreen,
            onLandingScreenChange = onLandingScreenChange,
        )

        Divider()

        DensityPreference(
            densityFraction = densityFraction,
            onDensityFractionChange = onDensityFractionChange,
        )
    }
}

@Composable
private fun DarkModePreference(
    darkModeEnabled: Boolean,
    onDarkThemeEnabledChange: (Boolean) -> Unit,
) {

    ListItem(
        headlineText = { Text("Dark Mode") },
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
    items: List<String>,
    selectedItem: String,
    onLandingScreenChange: (String) -> Unit,
) {

    ListItem(
        headlineText = { Text("Landing Screen") },
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
        headlineText = {

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
