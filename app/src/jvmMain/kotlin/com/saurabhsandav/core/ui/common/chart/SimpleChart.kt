package com.saurabhsandav.core.ui.common.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun SimpleChart(
    pageState: ChartPageState,
    legend: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = Modifier.fillMaxSize().then(modifier),
    ) {

        ChartPage(
            modifier = Modifier.matchParentSize(),
            state = pageState,
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(MaterialTheme.dimens.chartLegendPadding),
            propagateMinConstraints = true,
            content = legend,
        )
    }
}
