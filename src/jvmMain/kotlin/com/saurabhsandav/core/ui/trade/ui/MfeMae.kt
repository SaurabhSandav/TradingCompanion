package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.table.DefaultTableHeader
import com.saurabhsandav.core.ui.common.table.DefaultTableRow
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.rememberTableSchema
import com.saurabhsandav.core.ui.trade.model.TradeState.MfeAndMae

@Composable
internal fun MfeAndMae(mfeAndMae: MfeAndMae) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        val schema = rememberTableSchema<MfeAndMae> {
            addColumnText("Maximum Favorable Excursion") { it.mfePrice }
            addColumnText("Maximum Adverse Excursion") { it.maePrice }
        }

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            DefaultTableHeader(schema = schema)

            Divider()

            DefaultTableRow(
                item = mfeAndMae,
                schema = schema,
            )
        }
    }
}
