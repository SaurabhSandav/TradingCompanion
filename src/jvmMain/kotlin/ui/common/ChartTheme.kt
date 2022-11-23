package ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import chart.options.ChartOptions
import chart.options.GridLineOptions
import chart.options.GridOptions
import chart.options.LayoutOptions
import chart.options.common.Background
import ui.theme.isDark

val ChartLightModeOptions = ChartOptions(
    layout = LayoutOptions(
        background = Background.SolidColor(Color(0xFFFFFF)),
        textColor = Color(0x191919),
    ),
    grid = GridOptions(
        vertLines = GridLineOptions(
            color = Color(0xD6DCDE),
        ),
        horzLines = GridLineOptions(
            color = Color(0xD6DCDE),
        )
    ),
)

val ChartDarkModeOptions = ChartOptions(
    layout = LayoutOptions(
        background = Background.SolidColor(Color(0x222222)),
        textColor = Color(0xDDDDDD),
    ),
    grid = GridOptions(
        vertLines = GridLineOptions(
            color = Color(0x444444),
        ),
        horzLines = GridLineOptions(
            color = Color(0x444444),
        )
    ),
)

@Composable
fun themedChartOptions(): ChartOptions {

    val isDark = MaterialTheme.colorScheme.isDark()

    return when {
        isDark -> ChartDarkModeOptions
        else -> ChartLightModeOptions
    }
}
