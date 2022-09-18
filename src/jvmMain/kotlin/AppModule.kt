
import com.saurabhsandav.core.AppDB
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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
        val driver = JdbcSqliteDriver("jdbc:sqlite:/home/saurabh/Documents/Projects/DBs/TradingCompanion.db")
        AppDB.Schema.create(driver)
        AppDB(driver = driver)
    }
}
