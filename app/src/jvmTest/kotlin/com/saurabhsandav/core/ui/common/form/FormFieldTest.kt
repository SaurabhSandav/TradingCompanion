package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.snapshots.Snapshot
import com.saurabhsandav.core.ui.common.form.adapter.MutableStateFormField
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
    fun `Validate`() = runTest {

        val errorMessage = "Is Required"
        val field = MutableStateFormField("") { isRequired { errorMessage } }

        // Initial State
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }

        // Not valid
        assertFalse { field.validate().second }
        assertFalse(field.isValid)
        assertEquals(listOf(errorMessage), field.errorMessages)

        // Valid
        field.holder.value = "Test"
        assertTrue { field.validate().second }
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }
    }

    @Test
    fun `Auto Validate`() = runTest {

        val errorMessage = "Is Required"
        val field = MutableStateFormField("Test") { isRequired { errorMessage } }

        field.autoValidateIn(backgroundScope)
        delay(1.seconds)

        // Initial State
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }

        // Not valid
        field.holder.value = ""
        Snapshot.sendApplyNotifications()
        delay(1.seconds)
        assertFalse(field.isValid)
        assertEquals(listOf(errorMessage), field.errorMessages)

        // Valid
        field.holder.value = "Test"
        Snapshot.sendApplyNotifications()
        delay(1.seconds)
        assertTrue(field.isValid)
        assertTrue { field.errorMessages.isEmpty() }
    }

    @Test
    fun `Validate with Dependencies`() = runTest {

        val errorMessage = "Is Required"
        val field1 = MutableStateFormField("") { isRequired { errorMessage } }
        val field2 = MutableStateFormField("Test") {
            field1.validatedValue()
        }

        // Initial State
        assertTrue(field1.isValid)
        assertTrue { field1.errorMessages.isEmpty() }
        assertTrue(field2.isValid)
        assertTrue { field2.errorMessages.isEmpty() }

        // Dependency Not valid
        assertFalse { field2.validate().second }
        assertFalse(field1.isValid)
        assertTrue(field2.isValid)
        assertEquals(listOf(errorMessage), field1.errorMessages)
        assertTrue { field2.errorMessages.isEmpty() }

        // Dependency Valid
        field1.holder.value = "Test"
        assertTrue { field2.validate().second }
        assertTrue(field1.isValid)
        assertTrue(field2.isValid)
        assertTrue { field1.errorMessages.isEmpty() }
        assertTrue { field2.errorMessages.isEmpty() }
    }
}
