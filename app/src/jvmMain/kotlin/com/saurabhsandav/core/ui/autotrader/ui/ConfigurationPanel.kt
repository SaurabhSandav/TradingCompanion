package com.saurabhsandav.core.ui.autotrader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trades.model.AutoTraderScriptId
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderState.Script
import com.saurabhsandav.core.ui.autotrader.model.ConfigFormModel
import com.saurabhsandav.core.ui.autotrader.model.ScriptFormModel
import com.saurabhsandav.core.ui.common.state
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ConfigurationPanel(
    configFormModel: ConfigFormModel,
    scriptFormModel: ScriptFormModel?,
    scripts: ImmutableList<Script>,
    isScriptRunning: Boolean,
    onRun: () -> Unit,
    onNewScript: () -> Unit,
    onSelectScript: (AutoTraderScriptId) -> Unit,
    onCopyScript: (AutoTraderScriptId) -> Unit,
    onDeleteScript: (AutoTraderScriptId) -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxHeight().width(350.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        var selectedTab by state { Tabs.Controls }

        TabRow(
            selectedTabIndex = if (selectedTab == Tabs.Controls) 0 else 1,
            modifier = Modifier.fillMaxWidth(),
        ) {

            Tab(
                modifier = Modifier.height(48.dp),
                selected = selectedTab == Tabs.Controls,
                onClick = { selectedTab = Tabs.Controls },
                content = { Text("Controls") },
            )

            Tab(
                modifier = Modifier.height(48.dp),
                selected = selectedTab == Tabs.Scripts,
                onClick = { selectedTab = Tabs.Scripts },
                content = { Text("Scripts") },
            )
        }

        when (selectedTab) {
            Tabs.Controls -> Controls(
                configFormModel = configFormModel,
                scriptFormModel = scriptFormModel,
                isScriptRunning = isScriptRunning,
                onSelectScript = { selectedTab = Tabs.Scripts },
                onRun = onRun,
            )

            Tabs.Scripts -> Scripts(
                scripts = scripts,
                isSelected = { scriptFormModel?.id == it },
                onNewScript = onNewScript,
                onSelectScript = onSelectScript,
                onCopyScript = onCopyScript,
                onDeleteScript = onDeleteScript,
            )
        }
    }
}

private enum class Tabs {
    Controls,
    Scripts,
}
