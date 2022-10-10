package ui.common.form

import androidx.compose.runtime.saveable.SaverScope
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FormScopeTest {

    @Test
    fun `FormScope Saver Save_Restore`() {

        val initialText = "Initial"
        val updatedText = "Updated"
        val formScope = FormScope()

        val testTextField = formScope.textFieldState(
            initial = initialText,
            isErrorCheck = { false },
        )

        testTextField.onValueChange(updatedText)

        val saved = with(FormScope.Saver) {
            SaverScope { true }.save(formScope)
        }

        val restoredFormScope = saved?.let(FormScope.Saver::restore)

        val restoredTestTextField = restoredFormScope?.textFieldState(
            initial = initialText,
            isErrorCheck = { false },
        )

        assertEquals(restoredTestTextField?.value, updatedText)
    }
}
