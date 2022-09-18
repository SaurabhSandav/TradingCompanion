package main

import AppModule
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import closedtrades.ClosedTradesPresenter
import closedtrades.ClosedTradesScreen
import sizing.SizingPresenter
import sizing.SizingScreen

@Composable
internal fun MainScreen(
    appModule: AppModule,
) {

    var state by remember { mutableStateOf(1) }

    val titles = listOf("Trade Sizing", "Historical Trades")

    Column {

        TabRow(selectedTabIndex = state) {
            titles.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = state == index,
                    onClick = { state = index }
                )
            }
        }

        val sizingPresenter = remember { SizingPresenter(appModule) }
        val closedTradesPresenter = remember { ClosedTradesPresenter(appModule) }

        when (state) {
            0 -> SizingScreen(sizingPresenter)
            1 -> ClosedTradesScreen(closedTradesPresenter)
        }
    }
}
