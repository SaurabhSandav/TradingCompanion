package ui.trades

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import ui.common.ErrorSnackbar
import ui.common.app.LocalAppWindowState
import ui.trades.ui.TradesTable

@Composable
internal fun TradesScreen(
    presenter: TradesPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Trades" }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {

        TradesTable(
            tradesItems = state.tradesItems,
        )

        // Errors
        presenter.errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }
    }
}
