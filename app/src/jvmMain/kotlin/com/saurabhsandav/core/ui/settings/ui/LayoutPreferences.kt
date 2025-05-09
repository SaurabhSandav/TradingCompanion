package com.saurabhsandav.core.ui.settings.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen

@Composable
internal fun LayoutPreferences(
    darkModeEnabled: Boolean,
    onDarkThemeEnabledChange: (Boolean) -> Unit,
    landingScreen: LandingScreen,
    onLandingScreenChange: (LandingScreen) -> Unit,
    densityFraction: Float,
    onDensityFractionChange: (Float) -> Unit,
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
                onSelect = onLandingScreenChange,
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
