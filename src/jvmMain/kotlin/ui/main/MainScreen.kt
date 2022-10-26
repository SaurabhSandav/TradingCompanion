package ui.main

import AppModule
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    var state by state { 3 }

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

        Box(Modifier.fillMaxSize()) {

            val coroutineScope = rememberCoroutineScope()

            val accountPresenter = remember { ui.account.AccountPresenter(coroutineScope, appModule) }
            val sizingPresenter = remember { SizingPresenter(coroutineScope, appModule) }
            val openTradesPresenter = remember { OpenTradesPresenter(coroutineScope, appModule) }
            val closedTradesPresenter = remember { ClosedTradesPresenter(coroutineScope, appModule) }
            val studiesPresenter = remember { StudiesPresenter(coroutineScope, appModule) }

            AnimatedContent(state) { targetState ->

                when (targetState) {
                    0 -> AccountScreen(accountPresenter)
                    1 -> SizingScreen(sizingPresenter)
                    2 -> OpenTradesScreen(openTradesPresenter)
                    3 -> ClosedTradesScreen(closedTradesPresenter)
                    4 -> StudiesScreen(studiesPresenter)
                }
            }
        }
    }
}
