package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.DbUrlProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.uuid.Uuid

internal class TradingProfiles(
    private val appDispatchers: AppDispatchers,
    private val appPaths: AppPaths,
    private val dbUrlProvider: DbUrlProvider,
    private val appDB: AppDB,
) {

    private val records = mutableMapOf<ProfileId, TradingRecord>()
    private val recordBuilderMutex = Mutex()

    suspend fun newProfile(
        name: String,
        description: String,
        isTraining: Boolean,
    ): Flow<TradingProfile> = withContext(appDispatchers.IO) {
        appDB.transactionWithResult {

            // Insert into DB
            appDB.tradingProfileQueries.insert(
                name = name,
                description = description,
                path = generateProfilePath(),
                isTraining = isTraining,
            )

            // Get id of last inserted row
            val id = appDB.appDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::ProfileId)

            // Return TradingProfile
            appDB.tradingProfileQueries.get(id).asFlow().mapToOne(appDispatchers.IO)
        }
    }

    suspend fun updateProfile(
        id: ProfileId,
        name: String,
        description: String,
        isTraining: Boolean,
    ): Unit = withContext(appDispatchers.IO) {

        appDB.tradingProfileQueries.update(
            id = id,
            name = name,
            description = description,
            isTraining = isTraining,
        )
    }

    suspend fun copyProfile(
        id: ProfileId,
        name: (fromProfileName: String) -> String,
    ): Flow<TradingProfile> = withContext(appDispatchers.IO) {

        // Get profile details to copy
        val profile = appDB.tradingProfileQueries.get(id).executeAsOne()

        // Create new entry in DB
        appDB.transactionWithResult {

            val newProfilePath = generateProfilePath()

            // Insert into DB
            appDB.tradingProfileQueries.insert(
                name = name(profile.name),
                description = profile.description,
                path = newProfilePath,
                isTraining = profile.isTraining,
            )

            // Get id of last inserted row
            val newProfileId = appDB.appDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::ProfileId)

            // Set counts
            appDB.tradingProfileQueries.setTradeCounts(
                id = newProfileId,
                tradeCount = profile.tradeCount,
                tradeCountOpen = profile.tradeCountOpen,
            )

            // Copy associated files if exist
            if (profile.filesPath.exists()) {

                profile.filesPath.copyToRecursively(
                    target = appPaths.tradingRecordsPath.resolve(newProfilePath),
                    followLinks = false,
                    overwrite = false
                )
            }

            // Return TradingProfile
            return@transactionWithResult appDB.tradingProfileQueries
                .get(newProfileId)
                .asFlow()
                .mapToOne(appDispatchers.IO)
        }
    }

    suspend fun deleteProfile(id: ProfileId): Unit = withContext(appDispatchers.IO) {

        // Remove record
        records.remove(id)

        // Delete from DB
        val profile = appDB.tradingProfileQueries.get(id).executeAsOne()
        appDB.tradingProfileQueries.delete(id)

        // Delete associated files
        profile.filesPath.deleteRecursively()
    }

    val allProfiles: Flow<List<TradingProfile>> =
        appDB.tradingProfileQueries.getAll().asFlow().mapToList(appDispatchers.IO)

    fun getProfile(id: ProfileId): Flow<TradingProfile> {
        return getProfileOrNull(id).map { it ?: error("Profile($id) not found") }
    }

    fun getProfileOrNull(id: ProfileId): Flow<TradingProfile?> {
        return appDB.tradingProfileQueries.get(id).asFlow().mapToOneOrNull(appDispatchers.IO)
    }

    fun getDefaultProfile(): Flow<TradingProfile> {
        return with(appDB.tradingProfileQueries) {
            createDefaultIfEmpty()
            getDefault().asFlow().mapToOne(appDispatchers.IO)
        }
    }

    suspend fun exists(id: ProfileId): Boolean = withContext(appDispatchers.IO) {
        return@withContext appDB.tradingProfileQueries.exists(id).executeAsOne()
    }

    suspend fun isProfileNameUnique(
        name: String,
        ignoreProfileId: ProfileId? = null,
    ): Boolean = withContext(appDispatchers.IO) {
        return@withContext appDB.tradingProfileQueries
            .run {
                when {
                    ignoreProfileId == null -> isProfileNameUnique(name)
                    else -> isProfileNameUniqueIgnoreId(name, ignoreProfileId)
                }
            }
            .executeAsOne()
    }

    suspend fun getRecord(id: ProfileId): TradingRecord = recordBuilderMutex.withLock {

        records.getOrPut(id) {

            withContext(appDispatchers.IO) {

                val profile = appDB.tradingProfileQueries.get(id).executeAsOne()
                val profileFilesPath = profile.filesPath.createDirectories()

                TradingRecord(
                    appDispatchers = appDispatchers,
                    recordPath = profileFilesPath,
                    dbUrlProvider = dbUrlProvider,
                    onTradeCountsUpdated = { tradeCount, tradeCountOpen ->

                        appDB.tradingProfileQueries.setTradeCounts(
                            id = profile.id,
                            tradeCount = tradeCount,
                            tradeCountOpen = tradeCountOpen,
                        )
                    },
                )
            }
        }
    }

    private fun generateProfilePath(): String = Uuid.random().toString()

    private val TradingProfile.filesPath: Path
        get() = appPaths.tradingRecordsPath.resolve(path)
}
