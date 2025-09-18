package com.saurabhsandav.core.ui.common.form

import com.saurabhsandav.core.ui.common.form.ValidationResult.DependencyInvalid
import com.saurabhsandav.core.ui.common.form.ValidationResult.Invalid
import com.saurabhsandav.core.ui.common.form.ValidationResult.Valid
import com.saurabhsandav.core.ui.common.form.adapter.MutableStateFormField
import com.saurabhsandav.core.ui.common.form.validations.isInt
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("RemoveRedundantBackticks")
class ValidationTest {

    @Test
    fun `Valid`() = runTest {

        val validation = Validation<String> {
            isRequired()
        }
        val (result, dependencies) = runValidation("test", validation)

        assertIs<Valid>(result)
        assertEquals(emptySet(), dependencies)
    }

    @Test
    fun `Invalid`() = runTest {

        val validation = Validation<String> {
            isRequired()
        }
        val (result, dependencies) = runValidation("", validation)

        assertIs<Invalid>(result)
        assertEquals(listOf("Required"), result.errorMessages)
        assertEquals(emptySet(), dependencies)
    }

    @Test
    fun `Dependency Invalid`() = runTest {

        val formField = MutableStateFormField(-1) {
            if (this < 0) reportInvalid("Cannot be negative")
        }
        val validation = Validation<Int> {
            isRequired()
            if (formField.validatedValue() != this) reportInvalid("Not equal")
        }

        runValidation(3, validation).let { (result, dependencies) ->
            assertIs<DependencyInvalid>(result)
            assertEquals(setOf(formField), dependencies)
        }

        // Update dependee
        formField.holder.value = 3
        runValidation(3, validation).let { (result, dependencies) ->
            assertIs<Valid>(result)
            assertEquals(setOf(formField), dependencies)
        }
    }

    @Test
    fun `Multiple errors`() = runTest {

        val lengthErrorMessages = "Length required to be 6"
        val allNumbersErrorMessage = "Needs to be all numbers"

        val validation = Validation<String> {
            isRequired()
            collect {
                if (length != 6) reportInvalid(lengthErrorMessages)
                isInt { allNumbersErrorMessage }
            }
        }
        val (result, dependencies) = runValidation("Test", validation)

        assertIs<Invalid>(result)
        assertEquals(listOf(lengthErrorMessages, allNumbersErrorMessage), result.errorMessages)
        assertEquals(emptySet(), dependencies)
    }
}
