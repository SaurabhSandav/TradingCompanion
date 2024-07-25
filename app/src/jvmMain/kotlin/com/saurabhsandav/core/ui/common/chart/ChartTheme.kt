package com.saurabhsandav.core.ui.common.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.options.ChartOptions
import com.saurabhsandav.core.chart.options.ChartOptions.GridOptions
import com.saurabhsandav.core.chart.options.ChartOptions.GridOptions.GridLineOptions
import com.saurabhsandav.core.chart.options.ChartOptions.LayoutOptions
import com.saurabhsandav.core.chart.options.common.Background
import com.saurabhsandav.core.ui.theme.isDark

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
