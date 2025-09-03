package com.saurabhsandav.kbigdecimal

import app.softwork.serialization.csv.CSVFormat
import kotlinx.serialization.encodeToString
import java.math.MathContext
import java.math.RoundingMode
import kotlin.io.path.Path
import kotlin.io.path.writeText
import kotlin.math.pow
import kotlin.random.Random

private fun generateTestDataList(): List<TestData> = buildList {

    add(generateTestData("0", "1"))

    fun generateInterval(
        from: Double,
        until: Double,
    ) {

        repeat(10) {
            val first = Random.nextDouble(from, until).toString()
            val second = Random.nextDouble(from, until).toString()
            add(generateTestData(first, second))
        }
    }

    repeat(20) { exponent ->

        val lowerBound = 10.0.pow(exponent)
        val upperBound = 10.0.pow(exponent + 1)

        // --- Positive Numbers ---
        // Generates numbers between 10^n and 10^(n+1) (e.g., 10 to 100)
        generateInterval(lowerBound, upperBound)

        // --- Negative Numbers ---
        // Generates numbers between -10^(n+1) and -10^n (e.g., -100 to -10)
        // Note: The 'from' value must be less than the 'until' value in Random.nextDouble()
        generateInterval(-upperBound, -lowerBound)

        // --- Mixed Sign/Around Zero (for smaller exponents) ---
        // For smaller magnitudes, also generate numbers that cross zero.
        if (exponent < 10) { // Limit to smaller numbers (e.g., up to 10^10)
            // Generates numbers between -10^(n+1) and 10^(n+1) (e.g., -100 to 100)
            generateInterval(-upperBound, upperBound)
        }
    }
}

private fun generateTestData(
    first: String,
    second: String,
): TestData {

    val first = first.toBigDecimal()
    val second = second.toBigDecimal()

    return TestData(
        first = KBigDecimal(first),
        second = KBigDecimal(second),
        addition = (first + second).stripTrailingZeros().toPlainString(),
        additionPrecision2HalfEven = first.add(second, MathContext(2, RoundingMode.HALF_EVEN))
            .stripTrailingZeros()
            .toPlainString(),
        subtraction = (first - second).stripTrailingZeros().toPlainString(),
        subtractionPrecision3HalfEven = first.subtract(second, MathContext(3, RoundingMode.HALF_EVEN))
            .stripTrailingZeros()
            .toPlainString(),
        multiplication = (first * second).stripTrailingZeros().toPlainString(),
        multiplicationPrecision4HalfEven = first.multiply(second, MathContext(4, RoundingMode.HALF_EVEN))
            .stripTrailingZeros()
            .toPlainString(),
        division = first.divide(
            second,
            KBigDecimal.config.decimalPlaces,
            KBigDecimal.config.roundingMode.toPlatformRM(),
        ).stripTrailingZeros().toPlainString(),
        divisionPrecision5HalfEven = first.divide(second, MathContext(5, RoundingMode.HALF_EVEN))
            .stripTrailingZeros()
            .toPlainString(),
        divisionHalfEven = first.divide(second, KBigDecimal.config.decimalPlaces, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeUp = first.divide(second, 6, RoundingMode.UP)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeDown = first.divide(second, 6, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeCeiling = first.divide(second, 6, RoundingMode.CEILING)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeFloor = first.divide(second, 6, RoundingMode.FLOOR)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeHalfUp = first.divide(second, 6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeHalfDown = first.divide(second, 6, RoundingMode.HALF_DOWN)
            .stripTrailingZeros()
            .toPlainString(),
        divisionDecimalPlaces6RoundingModeHalfEven = first.divide(second, 6, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString(),
        remainder = first.remainder(second).stripTrailingZeros().toPlainString(),
        firstAbs = first.abs().stripTrailingZeros().toPlainString(),
        firstNegate = first.negate().stripTrailingZeros().toPlainString(),
        firstDecimalPlaces4HalfEven = first.setScale(4, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString(),
        comparison = first.compareTo(second),
        firstToString = first.toPlainString(),
        firstToDouble = first.toDouble(),
    )
}

private fun generateKtFile(csv: String): String = buildString {
    appendLine("package com.saurabhsandav.kbigdecimal")
    appendLine()
    appendLine("internal val TestDataCSV: String = \"\"\"")
    appendLine(csv)
    appendLine("\"\"\".trim()")
}

fun main(args: Array<String>) {

    val outPath = args.firstOrNull() ?: error("Output path not provided")

    val data = generateTestDataList()
    val csv = CSVFormat.encodeToString(data)

    val kotlinSource = generateKtFile(csv)

    Path(outPath).writeText(kotlinSource)
}
