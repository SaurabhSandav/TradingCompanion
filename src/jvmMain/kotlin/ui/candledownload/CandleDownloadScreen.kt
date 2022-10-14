package ui.candledownload

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ui.candledownload.model.CandleDownloadEvent
import ui.candledownload.model.CandleDownloadState
import ui.candledownload.ui.FyersLoginWebView

@Composable
internal fun CandleDownloadScreen(
    presenter: CandleDownloadPresenter,
) {

    val state by presenter.state.collectAsState()

    val loginState = state.login

    loginState ?: return

    when (loginState) {
        is CandleDownloadState.Login.NeedsToLogin -> FyersLoginWebView(loginState.loginUrl) {
            presenter.event(CandleDownloadEvent.LoginSuccess(it))
        }

        CandleDownloadState.Login.LoggedIn -> {

            Column {
                Text("Logged in!")
            }
        }
    }
}
