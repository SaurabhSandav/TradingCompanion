package ui.common

import AppDensityFraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.NoOpUpdate
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import java.awt.Component

@Composable
fun <T : Component> AppSwingPanel(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {

    // Workaround SwingPanel weird behaviour on density change
    val density = LocalDensity.current

    val newDensity = Density(density.density / AppDensityFraction, density.fontScale)

    CompositionLocalProvider(LocalDensity provides newDensity) {

        SwingPanel(
            background = background,
            factory = factory,
            modifier = modifier,
            update = update,
        )
    }
}
