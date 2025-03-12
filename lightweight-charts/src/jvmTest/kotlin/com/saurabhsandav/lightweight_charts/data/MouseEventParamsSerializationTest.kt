package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class MouseEventParamsSerializationTest {

    @Test
    fun test() {

        val input = """
            |{
            |  "time": 1722266400,
            |  "logical": 323,
            |  "point": {
            |    "x": 224,
            |    "y": 479
            |  },
            |  "paneIndex":0,
            |  "seriesData": {
            |    "candles": {
            |      "open": 24838.4,
            |      "high": 24841.65,
            |      "low": 24827.4,
            |      "close": 24840.4,
            |      "time": 1722266400
            |    },
            |    "ema9": {
            |      "value": 24832.61,
            |      "time": 1722266400
            |    },
            |    "ema21": {
            |      "value": 24837.17,
            |      "time": 1722266400
            |    }
            |  }
            |}
            |
        """.trimMargin()

        assertEquals(
            MouseEventParams(
                time = Time.UTCTimestamp(1722266400),
                logical = 323F,
                point = Point(x = 224F, y = 479F),
                paneIndex = 0,
                seriesData = mapOf(
                    "candles" to buildJsonObject {
                        put("open", 24838.4)
                        put("high", 24841.65)
                        put("low", 24827.4)
                        put("close", 24840.4)
                        put("time", 1722266400)
                    },
                    "ema9" to buildJsonObject {
                        put("value", 24832.61)
                        put("time", 1722266400)
                    },
                    "ema21" to buildJsonObject {
                        put("value", 24837.17)
                        put("time", 1722266400)
                    },
                ),
            ),
            LwcJson.decodeFromString<MouseEventParams>(input),
        )
    }
}
