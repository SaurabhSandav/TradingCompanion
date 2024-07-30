package com.saurabhsandav.core.ui.common.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.saurabhsandav.core.ui.theme.isDark
import com.saurabhsandav.lightweight_charts.options.ChartOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.GridOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.GridOptions.GridLineOptions
import com.saurabhsandav.lightweight_charts.options.ChartOptions.LayoutOptions
import com.saurabhsandav.lightweight_charts.options.common.Background
import kotlinx.css.Color

val ChartLightModeOptions = ChartOptions(
    layout = LayoutOptions(
        background = Background.SolidColor(Color("#FFFFFF")),
        textColor = Color("#191919"),
    ),
    grid = GridOptions(
        vertLines = GridLineOptions(
            color = Color("#D6DCDE"),
        ),
        horzLines = GridLineOptions(
            color = Color("#D6DCDE"),
        )
    ),
)

val ChartDarkModeOptions = ChartOptions(
    layout = LayoutOptions(
        background = Background.SolidColor(Color("#222222")),
        textColor = Color("#DDDDDD"),
    ),
    grid = GridOptions(
        vertLines = GridLineOptions(
            color = Color("#444444"),
        ),
        horzLines = GridLineOptions(
            color = Color("#444444"),
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
