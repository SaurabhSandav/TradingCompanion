package fyers.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponse(

    @SerialName("s")
    val s: String,

    @SerialName("candles")
    val candles: List<List<String>>,
)
