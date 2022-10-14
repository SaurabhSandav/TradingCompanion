package ui.candledownload.model

internal sealed class CandleDownloadEvent {

    data class LoginSuccess(val redirectionUrl: String) : CandleDownloadEvent()
}
