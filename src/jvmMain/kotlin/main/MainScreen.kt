package main

import AppModule
import account.AccountPresenter
import account.AccountScreen
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

    var state by remember { mutableStateOf(0) }

    val titles = listOf("Account", "Trade Sizing", "Historical Trades")

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

        val coroutineScope = rememberCoroutineScope()

        val accountPresenter = remember { AccountPresenter(coroutineScope, appModule) }
        val sizingPresenter = remember { SizingPresenter(coroutineScope, appModule) }
        val closedTradesPresenter = remember { ClosedTradesPresenter(coroutineScope, appModule) }

        when (state) {
            0 -> AccountScreen(accountPresenter)
            1 -> SizingScreen(sizingPresenter)
            2 -> ClosedTradesScreen(closedTradesPresenter)
        }
    }
}
