import com.russhwolf.settings.JvmPreferencesSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.AppDB
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import fyers_api.FyersApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import model.Account
import utils.AppPaths
import java.util.prefs.Preferences

internal class AppModule {

    val account: Flow<Account> = flowOf(
        Account(
            balance = 11250.toBigDecimal(),
            balancePerTrade = 11250.toBigDecimal(),
            leverage = 5.toBigDecimal(),
            riskAmount = 11250.toBigDecimal() * 0.02.toBigDecimal(),
        )
    )

    val appDB: AppDB = run {
        val driver = JdbcSqliteDriver("jdbc:sqlite:${AppPaths.getAppDataPath()}/${AppPaths.appName}.db")
        AppDB.Schema.create(driver)
        AppDB(driver = driver)
    }

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    val appPrefs = JvmPreferencesSettings(Preferences.userRoot().node(AppPaths.appName)).toFlowSettings()

    val fyersApiFactory = { FyersApi() }

    init {
//        TradeImporter(this).importTrades()
    }
}
