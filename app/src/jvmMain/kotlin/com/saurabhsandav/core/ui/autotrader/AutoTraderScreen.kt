package com.saurabhsandav.core.ui.autotrader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.AutoTraderScriptId
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.CopyScript
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.DeleteScript
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.FormatScript
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.NewScript
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.Run
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.SaveScript
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderEvent.SelectScript
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderState.Script
import com.saurabhsandav.core.ui.autotrader.model.ConfigFormModel
import com.saurabhsandav.core.ui.autotrader.model.ScriptFormModel
import com.saurabhsandav.core.ui.autotrader.ui.ConfigurationPanel
import com.saurabhsandav.core.ui.autotrader.ui.ScriptEditor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AutoTraderWindow(onCloseRequest: () -> Unit) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember { appModule.screensModule.autoTraderModule(scope).presenter() }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        preferredPlacement = WindowPlacement.Maximized,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "Auto Trader",
    ) {

        AutoTraderScreen(
            configFormModel = state.configFormModel,
            scripts = state.scripts,
            scriptFormModel = state.scriptFormModel,
            isScriptRunning = state.isScriptRunning,
            onRun = { state.eventSink(Run) },
            onNewScript = { state.eventSink(NewScript) },
            onSelectScript = { id -> state.eventSink(SelectScript(id)) },
            onCopyScript = { id -> state.eventSink(CopyScript(id)) },
            onDeleteScript = { id -> state.eventSink(DeleteScript(id)) },
            onFormatScript = { state.eventSink(FormatScript) },
            onSaveScript = { state.eventSink(SaveScript) },
        )
    }
}

@Composable
private fun AutoTraderScreen(
    configFormModel: ConfigFormModel,
    scripts: ImmutableList<Script>,
    scriptFormModel: ScriptFormModel?,
    isScriptRunning: Boolean,
    onRun: () -> Unit,
    onNewScript: () -> Unit,
    onSelectScript: (AutoTraderScriptId) -> Unit,
    onCopyScript: (AutoTraderScriptId) -> Unit,
    onDeleteScript: (AutoTraderScriptId) -> Unit,
    onFormatScript: () -> Unit,
    onSaveScript: () -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        ConfigurationPanel(
            configFormModel = configFormModel,
            scriptFormModel = scriptFormModel,
            scripts = scripts,
            isScriptRunning = isScriptRunning,
            onRun = onRun,
            onNewScript = onNewScript,
            onSelectScript = onSelectScript,
            onCopyScript = onCopyScript,
            onDeleteScript = onDeleteScript,
        )

        when {
            scriptFormModel != null -> ScriptEditor(
                formModel = scriptFormModel,
                onFormatScript = onFormatScript,
                onSaveScript = onSaveScript,
            )

            else -> Box(Modifier.fillMaxHeight().weight(1F)) {

                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "No Script selected",
                )
            }
        }
    }
}
