package ui.settings

import AppModule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ListItem
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.controls.ListSelectionField
import ui.common.state
import ui.landing.model.LandingScreen
import ui.settings.model.SettingsEvent

@Composable
internal fun SettingsScreen(appModule: AppModule) {

    val scope = rememberCoroutineScope()
    val presenter = remember { SettingsPresenter(scope, appModule) }
    val state by presenter.state.collectAsState()

    SettingsScreen(
        landingScreen = state.landingScreen,
        onLandingScreenChange = { presenter.event(SettingsEvent.ChangeLandingScreen(it)) },
        densityFraction = state.densityFraction,
        onDensityFractionChange = { presenter.event(SettingsEvent.ChangeDensityFraction(it)) },
    )
}

@Composable
internal fun SettingsScreen(
    landingScreen: String,
    onLandingScreenChange: (String) -> Unit,
    densityFraction: Float,
    onDensityFractionChange: (Float) -> Unit,
) {

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

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
private fun LandingScreenPreference(
    items: List<String>,
    selectedItem: String,
    onLandingScreenChange: (String) -> Unit,
) {

    ListItem(
        text = { Text("Landing Screen") },
        trailing = {

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
        text = {

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
