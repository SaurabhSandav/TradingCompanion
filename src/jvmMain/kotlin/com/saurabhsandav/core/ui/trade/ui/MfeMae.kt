package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.trade.model.TradeState

@Composable
internal fun MfeAndMae(mfeAndMae: TradeState.MfeAndMae) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                modifier = Modifier.weight(1F),
                text = "Maximum Favorable Excursion",
                textAlign = TextAlign.Center,
            )

            Text(
                modifier = Modifier.weight(1F),
                text = "Maximum Adverse Excursion",
                textAlign = TextAlign.Center,
            )
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                modifier = Modifier.weight(1F),
                text = mfeAndMae.mfePrice,
                textAlign = TextAlign.Center,
            )

            Text(
                modifier = Modifier.weight(1F),
                text = mfeAndMae.maePrice,
                textAlign = TextAlign.Center,
            )
        }
    }
}
