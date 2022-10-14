package ui.candledownload.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import studies.Study

@Immutable
internal data class CandleDownloadState(
    val login: Login? = null,
) {

    sealed class Login {
        data class NeedsToLogin(val loginUrl: String) : Login()
        object LoggedIn : Login()
    }
}
