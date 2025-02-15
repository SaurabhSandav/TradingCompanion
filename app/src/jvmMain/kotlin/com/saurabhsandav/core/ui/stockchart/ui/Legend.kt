package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.saurabhsandav.core.ui.common.chart.legend.Legend
import com.saurabhsandav.core.ui.common.chart.legend.LegendItem
import com.saurabhsandav.core.ui.common.chart.legend.LegendVisibilityButton
import com.saurabhsandav.core.ui.stockchart.PlotterManager

@Composable
fun Legend(
    plotterManager: PlotterManager,
) {

    Legend {

        plotterManager.plotters.forEach { plotter ->

            key(plotter) {

                LegendItem(
                    label = { Text(plotter.legendLabel) },
                    controls = {

                        LegendVisibilityButton(
                            isEnabled = plotter.isEnabled,
                            onToggleIsEnabled = { plotterManager.setPlotterEnabled(plotter, !plotter.isEnabled) },
                        )
                    },
                    values = { Text(plotter.legendText) },
                )
            }
        }

        LegendItem(
            label = { Text("Markers") },
            controls = {

                val isEnabled by plotterManager.areMarkersEnabled.collectAsState(false)

                LegendVisibilityButton(
                    isEnabled = isEnabled,
                    onToggleIsEnabled = { plotterManager.setMarkersEnabled(!isEnabled) },
                )
            },
            values = {},
        )
    }
}
