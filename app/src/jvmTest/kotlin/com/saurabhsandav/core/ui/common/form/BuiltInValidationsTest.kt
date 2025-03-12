package com.saurabhsandav.core.ui.common.form

import com.saurabhsandav.core.ui.common.form.ValidationResult.Invalid
import com.saurabhsandav.core.ui.common.form.ValidationResult.Valid
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isPositive
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("RemoveRedundantBackticks")
class BuiltInValidationsTest {

    @Test
    fun `String is required`() = runTest {

        val validation = Validation<String> { isRequired() }

        runValidation("", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Required"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("test", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `String is not required`() = runTest {

        val validation = Validation<String> { isRequired(false) }

        runValidation("", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("test", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `Nullable generic is required`() = runTest {

        val validation = Validation<Int?> { isRequired() }

        runValidation(null, validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Required"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation(2, validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `Nullable generic is not required`() = runTest {

        val validation = Validation<Int?> { isRequired(false) }

        runValidation(null, validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }

        runValidation(2, validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `Int`() = runTest {

        val validation = Validation<String> { isInt() }

        runValidation("", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid integer"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("123.456", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid integer"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("test", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid integer"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("123", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `Int is positive`() = runTest {

        val validation = Validation<String> { isInt()?.isPositive() }

        runValidation("", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid integer"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("123.456", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid integer"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("test", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid integer"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("-123", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Cannot be 0 or negative"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("123", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `BigDecimal`() = runTest {

        val validation = Validation<String> { isBigDecimal() }

        runValidation("", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid number"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("test", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid number"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("123.456", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }

    @Test
    fun `BigDecimal is positive`() = runTest {

        val validation = Validation<String> { isBigDecimal()?.isPositive() }

        runValidation("", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid number"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("test", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Not a valid number"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("-123.456", validation).let { (result, dependencies) ->
            assertIs<Invalid>(result)
            assertEquals(listOf("Cannot be 0 or negative"), result.errorMessages)
            assertEquals(emptySet(), dependencies)
        }

        runValidation("123.456", validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(emptySet(), dependencies)
        }
    }
}
