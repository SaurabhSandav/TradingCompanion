package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.snapshots.Snapshot
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@Suppress("RemoveRedundantBackticks")
class FormFieldTest {

    @Test
    fun `Set value`() {

        val field = FormField("", null)

        assertEquals("", field.value)

        field.value = "Test1"
        assertEquals("Test1", field.value)

        field.value = "Test2"
        assertEquals("Test2", field.value)
    }

    @Test
    fun `Validate`() = runTest {

        val errorMessage = "Is Required"
        val field = FormField("") { isRequired { errorMessage } }

        // Initial State
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }

        // Not valid
        assertFalse { field.validate() }
        assertFalse(field.isValid)
        assertEquals(listOf(errorMessage), field.errorMessages)

        // Valid
        field.value = "Test"
        assertTrue { field.validate() }
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }
    }

    @Test
    fun `Auto Validate`() = runTest {

        val errorMessage = "Is Required"
        val field = FormField("Test") { isRequired { errorMessage } }

        field.autoValidateIn(backgroundScope)
        delay(1.seconds)

        // Initial State
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }

        // Not valid
        field.value = ""
        Snapshot.sendApplyNotifications()
        delay(1.seconds)
        assertFalse(field.isValid)
        assertEquals(listOf(errorMessage), field.errorMessages)

        // Valid
        field.value = "Test"
        Snapshot.sendApplyNotifications()
        delay(1.seconds)
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }
    }

    @Test
    fun `Validate with Dependencies`() = runTest {

        val errorMessage = "Is Required"
        val field1 = FormField("") { isRequired { errorMessage } }
        val field2 = FormField("Test") {
            field1.validatedValue()
        }

        // Initial State
        assertTrue(field1.isValid)
        assertTrue { field1.errorMessages.isEmpty() }
        assertTrue(field2.isValid)
        assertTrue { field2.errorMessages.isEmpty() }

        // Dependency Not valid
        assertFalse { field2.validate() }
        assertFalse(field1.isValid)
        assertTrue(field2.isValid)
        assertEquals(listOf(errorMessage), field1.errorMessages)
        assertTrue { field2.errorMessages.isEmpty() }

        // Dependency Valid
        field1.value = "Test"
        assertTrue { field2.validate() }
        assertTrue(field1.isValid)
        assertTrue(field2.isValid)
        assertTrue { field1.errorMessages.isEmpty() }
        assertTrue { field2.errorMessages.isEmpty() }
    }
}
