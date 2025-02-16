package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun BoxScope.ChartOverlay(
    layout: ChartsLayout,
    modifier: Modifier,
    legend: @Composable (Int) -> Unit,
) {

    Layout(
        modifier = modifier,
        content = {

            layout.rects.forEachIndexed { chartIndex, rect ->

                key(chartIndex) {

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(MaterialTheme.dimens.chartLegendPadding),
                    ) {
                        legend(chartIndex)
                    }
                }
            }
        },
    ) { measurables, constraints ->

        val size = Size(
            width = constraints.maxWidth.toFloat(),
            height = constraints.maxHeight.toFloat(),
        )

        val rects = layout.getComposeRects(size)

        val placeables = measurables.mapIndexed { index, measurable ->

            val rect = rects[index]
            val width = rect.width.toInt()
            val height = rect.height.toInt()

            measurable.measure(
                Constraints(
                    minWidth = width,
                    minHeight = height,
                    maxWidth = width,
                    maxHeight = height,
                ),
            )
        }

        layout(constraints.maxWidth, constraints.maxHeight) {

            placeables.mapIndexed { index, placeable ->

                val rect = rects[index]

                placeable.place(rect.left.toInt(), rect.top.toInt())
            }
        }
    }
}
