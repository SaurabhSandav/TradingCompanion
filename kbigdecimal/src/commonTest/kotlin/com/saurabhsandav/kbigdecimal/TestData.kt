package com.saurabhsandav.kbigdecimal

import app.softwork.serialization.csv.CSVFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class TestData(
    val first: KBigDecimal,
    val second: KBigDecimal,
    val addition: String,
    val additionPrecision2HalfEven: String,
    val subtraction: String,
    val subtractionPrecision3HalfEven: String,
    val multiplication: String,
    val multiplicationPrecision4HalfEven: String,
    val division: String,
    val divisionPrecision5HalfEven: String,
    val divisionHalfEven: String,
    val divisionDecimalPlaces6RoundingModeUp: String,
    val divisionDecimalPlaces6RoundingModeDown: String,
    val divisionDecimalPlaces6RoundingModeCeiling: String,
    val divisionDecimalPlaces6RoundingModeFloor: String,
    val divisionDecimalPlaces6RoundingModeHalfUp: String,
    val divisionDecimalPlaces6RoundingModeHalfDown: String,
    val divisionDecimalPlaces6RoundingModeHalfEven: String,
    val remainder: String,
    val firstAbs: String,
    val firstNegate: String,
    val firstDecimalPlaces4HalfEven: String,
    val comparison: Int,
    val firstToString: String,
    val firstToDouble: Double,
)

fun loadTestData(): List<TestData> = CSVFormat.decodeFromString(TestDataCSV)
