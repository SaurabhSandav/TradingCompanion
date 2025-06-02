package com.saurabhsandav.core.ui.common.form

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FormModelTest {

    @Test
    fun `Add-Attach-Remove Field`() = runTest {

        val formModel = FormModel()
        val field = object : FormField<String> by FormFieldImpl("", null) {

            var coroutineScope: CoroutineScope? = null

            override fun autoValidateIn(coroutineScope: CoroutineScope) {
                this.coroutineScope = coroutineScope
            }
        }

        // Add field
        formModel.addField(field)

        // Attach
        formModel.onAttach(backgroundScope)
        val coroutineScope = assertNotNull(field.coroutineScope)
        assertTrue { coroutineScope.isActive }

        // Remove
        formModel.removeField(field)
        assertFalse { coroutineScope.isActive }
    }

    @Test
    fun `Attach-Detach Field`() = runTest {

        val formModel = FormModel()
        val field = object : FormField<String> by FormFieldImpl("", null) {

            var coroutineScope: CoroutineScope? = null

            override fun autoValidateIn(coroutineScope: CoroutineScope) {
                this.coroutineScope = coroutineScope
            }
        }

        // Add field
        formModel.addField(field)

        // Attach
        formModel.onAttach(backgroundScope)
        val coroutineScope = assertNotNull(field.coroutineScope)
        assertTrue { coroutineScope.isActive }

        // Detach
        formModel.onDetach()
        assertFalse { coroutineScope.isActive }
    }
}
