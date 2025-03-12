package com.saurabhsandav.lightweight_charts.options.common

import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.css.Color
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("RemoveRedundantBackticks")
class BackgroundSerializationTest {

    @Test
    fun `SolidColor`() {

        val value = Background.SolidColor(Color.red)

        assertEquals(
            buildJsonObject {
                put("type", "ColorType.Solid")
                put("color", value.color.value)
            },
            LwcJson.encodeToJsonElement<Background>(value),
        )
    }

    @Test
    fun `VerticalGradientColor`() {

        val value = Background.VerticalGradientColor(
            topColor = Color.red,
            bottomColor = Color.green,
        )

        assertEquals(
            buildJsonObject {
                put("type", "ColorType.VerticalGradient")
                put("topColor", value.topColor!!.value)
                put("bottomColor", value.bottomColor!!.value)
            },
            LwcJson.encodeToJsonElement<Background>(value),
        )
    }
}
