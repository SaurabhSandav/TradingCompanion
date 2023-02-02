package com.saurabhsandav.core.ui.common.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.NoOpUpdate
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.saurabhsandav.core.LocalDensityFraction
import java.awt.Component

@Composable
fun <T : Component> AppSwingPanel(
    background: Color = MaterialTheme.colorScheme.background,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {

    // Workaround SwingPanel weird behaviour on density change
    val density = LocalDensity.current
    val densityFraction = LocalDensityFraction.current

    val newDensity = Density(density.density / densityFraction, density.fontScale)

    CompositionLocalProvider(LocalDensity provides newDensity) {

        SwingPanel(
            background = background,
            factory = factory,
            modifier = modifier,
            update = update,
        )
    }
}

@Stable
class AppWindowState {

    var title by mutableStateOf("Untitled")
}

val LocalAppWindowState = compositionLocalOf { AppWindowState() }
