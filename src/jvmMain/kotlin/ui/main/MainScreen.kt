package ui.main

import AppModule
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import ui.account.AccountScreen
import ui.closedtrades.ClosedTradesPresenter
import ui.closedtrades.ClosedTradesScreen
import ui.common.state
import ui.opentrades.OpenTradesPresenter
import ui.opentrades.OpenTradesScreen
import ui.sizing.SizingPresenter
import ui.sizing.SizingScreen
import ui.studies.StudiesPresenter
import ui.studies.StudiesScreen

@Composable
internal fun MainScreen(
    appModule: AppModule,
) {

    var state by state { 4 }

    val titles = listOf("Account", "Trade Sizing", "Open Trades", "Historical Trades", "Studies")

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

        val accountPresenter = remember { ui.account.AccountPresenter(coroutineScope, appModule) }
        val sizingPresenter = remember { SizingPresenter(coroutineScope, appModule) }
        val openTradesPresenter = remember { OpenTradesPresenter(coroutineScope, appModule) }
        val closedTradesPresenter = remember { ClosedTradesPresenter(coroutineScope, appModule) }
        val studiesPresenter = remember { StudiesPresenter(coroutineScope, appModule) }

        when (state) {
            0 -> AccountScreen(accountPresenter)
            1 -> SizingScreen(sizingPresenter)
            2 -> OpenTradesScreen(openTradesPresenter)
            3 -> ClosedTradesScreen(closedTradesPresenter)
            4 -> StudiesScreen(studiesPresenter)
        }
    }
}
