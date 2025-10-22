package com.saurabhsandav.kbigdecimal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KBigDecimalTest {

    val dataList = loadTestData()

    @Test
    fun construction() {

        assertFailsWith<NumberFormatException> { "".toKBigDecimal() }
        assertFailsWith<NumberFormatException> { "asd".toKBigDecimal() }
        assertFailsWith<NumberFormatException> { Double.NaN.toKBigDecimal() }
        assertFailsWith<NumberFormatException> { Double.POSITIVE_INFINITY.toKBigDecimal() }
        assertFailsWith<NumberFormatException> { Double.NEGATIVE_INFINITY.toKBigDecimal() }

        assertNull("".toKBigDecimalOrNull())
        assertNull("asd".toKBigDecimalOrNull())

        assertEquals("50.00".toKBigDecimal().toString(), "50")
    }

    @Test
    fun `To String`() {

        dataList.forEach { data ->
            assertEquals(data.firstToString, data.first.toString())
        }
    }

    @Test
    fun addition() {

        dataList.forEach { data ->
            assertEquals(data.addition, (data.first + data.second).toString())
            assertEquals(
                data.additionPrecision2HalfEven,
                data.first.plus(data.second, KMathContext(2, KRoundingMode.HalfEven)).toString(),
            )
        }
    }

    @Test
    fun subtraction() {

        dataList.forEach { data ->
            assertEquals(data.subtraction, (data.first - data.second).toString())
            assertEquals(
                data.subtractionPrecision3HalfEven,
                data.first.minus(data.second, KMathContext(3, KRoundingMode.HalfEven)).toString(),
            )
        }
    }

    @Test
    fun multiplication() {

        dataList.forEach { data ->
            assertEquals(data.multiplication, (data.first * data.second).toString())
            assertEquals(
                data.multiplicationPrecision4HalfEven,
                data.first.times(data.second, KMathContext(4, KRoundingMode.HalfEven)).toString(),
            )
        }
    }

    @Test
    fun division() {

        dataList.forEach { data ->

            assertEquals(data.division, (data.first / data.second).toString())

            assertEquals(
                data.divisionPrecision5HalfEven,
                data.first.div(data.second, KMathContext(5, KRoundingMode.HalfEven)).toString(),
            )

            assertEquals(data.divisionHalfEven, data.first.div(data.second, KRoundingMode.HalfEven).toString())
        }
    }

    @Test
    fun remainder() {

        dataList.forEach { data ->

            assertEquals(data.remainder, (data.first.remainder(data.second)).toString())
        }
    }

    @Test
    fun `Rounding Modes`() {

        dataList.forEach { data ->

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeUp,
                data.first.div(data.second, 6, KRoundingMode.Up).toString(),
            )

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeDown,
                data.first.div(data.second, 6, KRoundingMode.Down).toString(),
            )

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeCeiling,
                data.first.div(data.second, 6, KRoundingMode.Ceiling).toString(),
            )

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeFloor,
                data.first.div(data.second, 6, KRoundingMode.Floor).toString(),
            )

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeHalfUp,
                data.first.div(data.second, 6, KRoundingMode.HalfUp).toString(),
            )

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeHalfDown,
                data.first.div(data.second, 6, KRoundingMode.HalfDown).toString(),
            )

            assertEquals(
                data.divisionDecimalPlaces6RoundingModeHalfEven,
                data.first.div(data.second, 6, KRoundingMode.HalfEven).toString(),
            )
        }
    }

    @Test
    fun abs() {

        dataList.forEach { data ->
            assertEquals(data.firstAbs, data.first.abs().toString())
        }
    }

    @Test
    fun negated() {

        dataList.forEach { data ->
            assertEquals(data.firstNegate, data.first.negated().toString())
        }
    }

    @Test
    fun `decimalPlaces() - 4, Half Even`() {

        dataList.forEach { data ->

            assertEquals(
                data.firstDecimalPlaces4HalfEven,
                data.first.decimalPlaces(4, KRoundingMode.HalfEven).toString(),
            )
        }
    }

    @Test
    fun compareTo() {

        dataList.forEach { data ->
            assertEquals(data.comparison, data.first.compareTo(data.second))
        }
    }

    @Test
    fun toDouble() {

        dataList.forEach { data ->
            assertEquals(data.firstToDouble, data.first.toDouble())
        }
    }

    @Test
    fun isEqualTo() {

        dataList.forEach { data ->
            assertTrue { data.first.isEqualTo(data.firstToString.toKBigDecimal()) }
        }
    }

    @Test
    fun `Divide By 0`() {
        assertFailsWith<ArithmeticException> { KBigDecimal.Zero / KBigDecimal.Zero }
    }
}
