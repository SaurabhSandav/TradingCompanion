package ui.main

import AppModule
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ui.account.AccountScreen
import ui.closedtrades.ClosedTradesPresenter
import ui.closedtrades.ClosedTradesScreen
import ui.common.Tooltip
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

    var selectedItem by state { 3 }

    val items = listOf(
        "Account" to Icons.Filled.AccountBalance,
        "Trade Sizing" to Icons.Filled.Calculate,
        "Open Trades" to Icons.Filled.FolderOpen,
        "Closed Trades" to Icons.Filled.Folder,
        "Studies" to Icons.Filled.FactCheck,
    )

    Row {

        NavigationRail(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {

            items.forEachIndexed { index, item ->

                TooltipArea(
                    tooltip = { Tooltip(item.first) },
                ) {

                    NavigationRailItem(
                        icon = { Icon(item.second, contentDescription = item.first) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }

        Box(Modifier.fillMaxSize()) {

            val coroutineScope = rememberCoroutineScope()

            val accountPresenter = remember { ui.account.AccountPresenter(coroutineScope, appModule) }
            val sizingPresenter = remember { SizingPresenter(coroutineScope, appModule) }
            val openTradesPresenter = remember { OpenTradesPresenter(coroutineScope, appModule) }
            val closedTradesPresenter = remember { ClosedTradesPresenter(coroutineScope, appModule) }
            val studiesPresenter = remember { StudiesPresenter(coroutineScope, appModule) }

            AnimatedContent(selectedItem) { targetState ->

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
