package fyers_api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Quotes(

  @SerialName("d")
  val quote: List<Quote>,
)

@Serializable
data class Quote(

  @SerialName("n")
  val symbol: String,

  @SerialName("s")
  val s: String,

  @SerialName("v")
  val quoteData: QuoteData,
)

@Serializable
data class QuoteData(

  @SerialName("ch")
  val changeValue: Double,

  @SerialName("chp")
  val changePercent: Double,

  @SerialName("lp")
  val lastTradedPrice: Double,

  @SerialName("spread")
  val spread: Double,

  @SerialName("ask")
  val ask: Double,

  @SerialName("bid")
  val bid: Double,

  @SerialName("open_price")
  val openPrice: Double,

  @SerialName("high_price")
  val highPrice: Double,

  @SerialName("low_price")
  val lowPrice: Double,

  @SerialName("prev_close_price")
  val prevClosePrice: Double,

  @SerialName("volume")
  val volume: Int,

  @SerialName("short_name")
  val shortName: String,

  @SerialName("exchange")
  val exchange: String,

  @SerialName("description")
  val description: String,

  @SerialName("original_name")
  val originalName: String,

  @SerialName("symbol")
  val symbol: String,

  @SerialName("fyToken")
  val fyToken: String,

  @SerialName("tt")
  val todaysTime: Int,

  @SerialName("cmd")
  val cmd: Cmd,
)

@Serializable
data class Cmd(

  @SerialName("t")
  val currentEpoch: Int,

  @SerialName("o")
  val open: Double,

  @SerialName("h")
  val high: Double,

  @SerialName("l")
  val low: Double,

  @SerialName("c")
  val close: Double,

  @SerialName("v")
  val volume: Int,

  @SerialName("tf")
  val timestamp: String,
)
