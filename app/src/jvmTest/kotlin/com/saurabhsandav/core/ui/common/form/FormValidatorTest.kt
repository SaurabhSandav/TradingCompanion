package com.saurabhsandav.core.ui.common.form

import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormValidatorTest {

    @Test
    fun `Validate - No FormModels`() = runTest {

        val validator = FormValidator(backgroundScope)

        assertFormValid(validator)

        // Validate
        assertTrue { validator.validate() }
        assertFormValid(validator)
    }

    @Test
    fun `Validate - Invalid and then Valid`() = runTest {

        val formModel = FormModel()
        val textField = formModel.addField("") { isRequired() }
        val validator = FormValidator(backgroundScope).apply { addModel(formModel) }

        assertFormValid(validator)

        // Validate
        assertFalse { validator.validate() }
        assertFormInvalid(validator)

        // Fix invalid field and validate
        textField.value = "Test"
        assertTrue { validator.validate() }
        assertFormValid(validator)
    }

    @Test
    fun `Submit - No FormModels`() = runTest {

        val validator = FormValidator(backgroundScope)

        assertFormValid(validator)

        // Validate
        validator.submit()
        yield()
        assertFormValid(validator)
    }

    @Test
    fun `Submit - Invalid and then Valid`() = runTest {

        val formModel = FormModel()
        val textField = formModel.addField("") { isRequired() }
        val validator = FormValidator(backgroundScope).apply { addModel(formModel) }

        assertFormValid(validator)

        // Validate
        validator.submit()
        yield()
        assertFormInvalid(validator)

        // Fix invalid field and validate
        textField.value = "Test"
        validator.submit()
        yield()

        assertFormValid(validator)
    }

    @Test
    fun `Submit - Multiple FormModels - Invalid and then Valid`() = runTest {

        val formModel1 = FormModel()
        val field1 = formModel1.addField("") { isRequired() }
        val validator = FormValidator(backgroundScope).apply {
            addModel(formModel1)
            val formModel2 = FormModel().apply {
                addField("Field2") { isRequired() }
            }
            addModel(formModel2)
        }

        // Validate
        validator.submit()
        yield()
        assertFormInvalid(validator)

        // Fix invalid field and validate
        field1.value = "Field1"
        validator.submit()
        yield()
        assertFormValid(validator)
    }

    @Test
    fun `Remove FormModel with invalid field`() = runTest {

        val formModel1 = FormModel().apply {
            addField("Field1") { isRequired() }
        }
        val formModel2 = FormModel().apply {
            addField("") { isRequired() }
        }
        val validator = FormValidator(backgroundScope).apply {
            addModel(formModel1)
            addModel(formModel2)
        }

        // Validate
        validator.submit()
        yield()
        assertFormInvalid(validator)

        // Remove FormModel with invalid field and validate
        validator.removeModel(formModel2)
        validator.submit()
        yield()
        assertFormValid(validator)
    }

    @Test
    fun `Add FormModel with invalid field`() = runTest {

        val formModel1 = FormModel().apply {
            addField("Field1") { isRequired() }
        }
        val formModel2 = FormModel().apply {
            addField("") { isRequired() }
        }
        val validator = FormValidator(backgroundScope).apply {
            addModel(formModel1)
        }

        // Validate
        validator.submit()
        yield()
        assertFormValid(validator)

        // Remove FormModel with invalid field and validate
        validator.addModel(formModel2)
        validator.submit()
        yield()
        assertFormInvalid(validator)
    }

    private fun assertFormValid(validator: FormValidator) {
        assertTrue(validator.isValid)
        assertTrue(validator.canSubmit)
    }

    private fun assertFormInvalid(validator: FormValidator) {
        assertFalse(validator.isValid)
        assertFalse(validator.canSubmit)
    }
}
