package com.saurabhsandav.core.ui.autotrader

import com.saurabhsandav.core.trading.Candle
import java.math.BigDecimal
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

@KotlinScript(
    fileExtension = "autotrader.kts",
    compilationConfiguration = AutoTraderScriptConfiguration::class
)
abstract class AutoTraderScriptTemplate(
    scriptStrategyGenerator: ScriptStrategyGenerator,
) : ScriptStrategyGenerator by scriptStrategyGenerator

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object AutoTraderScriptConfiguration : ScriptCompilationConfiguration({

    defaultImports(
        BigDecimal::class,
    )

    defaultImports(
        "com.saurabhsandav.core.trades.model.*",
        "com.saurabhsandav.core.trading.*",
        "com.saurabhsandav.core.trading.backtest.*",
        "kotlinx.datetime.*",
    )
})

interface ScriptStrategyGenerator {

    fun strategy(block: suspend (TradingStrategy.Environment, Candle) -> Unit)
}

class AutoTraderScriptHost {

    private val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<AutoTraderScriptTemplate> {
        jvm {
            // Script will have access to everything in the classpath
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    }

    fun execStr(
        scriptStrategyGenerator: ScriptStrategyGenerator,
        script: String,
    ): ResultWithDiagnostics<EvaluationResult> {

        return BasicJvmScriptingHost().eval(
            script = script.toScriptSource(),
            compilationConfiguration = compilationConfiguration,
            evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<AutoTraderScriptTemplate> {
                constructorArgs(scriptStrategyGenerator)
            },
        )
    }
}
