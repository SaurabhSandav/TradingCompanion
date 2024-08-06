package com.saurabhsandav.lightweight_charts.options.common

import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceFormatSerializationTest {

    @Test
    fun BuiltIn() {

        val value = PriceFormat.BuiltIn(PriceFormat.Type.Price, minMove = 3.0)

        assertEquals(
            buildJsonObject {
                put("type", "price")
                put("minMove", 3.0)
            },
            LwcJson.encodeToJsonElement<PriceFormat>(value),
        )
    }

    @Test
    fun Custom() {

        val value = PriceFormat.Custom(2.0)

        assertEquals(
            buildJsonObject {
                put("type", "custom")
                put("minMove", 2.0)
            },
            LwcJson.encodeToJsonElement<PriceFormat>(value),
        )
    }
}
