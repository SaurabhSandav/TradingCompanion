package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("RemoveRedundantBackticks")
class TimeSerializationTest {

    @Test
    fun `UTCTimeStamp`() {

        val value = Time.UTCTimestamp(1529899200)

        assertEquals(JsonPrimitive(value.value), LwcJson.encodeToJsonElement<Time>(value))
    }

    @Test
    fun `BusinessDay`() {

        val value = Time.BusinessDay(year = 2019, month = 6, day = 1)

        assertEquals(
            buildJsonObject {
                put("year", 2019)
                put("month", 6)
                put("day", 1)
            },
            LwcJson.encodeToJsonElement<Time>(value),
        )
    }

    @Test
    fun `String`() {

        val value = Time.ISOString("2021-02-03")

        assertEquals(JsonPrimitive(value.value), LwcJson.encodeToJsonElement<Time>(value))
    }
}
