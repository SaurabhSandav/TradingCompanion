package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.*
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

internal class TradingProfiles(
    private val appDispatchers: AppDispatchers,
    private val appFilesPath: Path,
    private val appDB: AppDB,
) {

    private val records = mutableMapOf<ProfileId, TradingRecord>()
    private val recordBuilderMutex = Mutex()

    suspend fun newProfile(
        name: String,
        description: String,
        isTraining: Boolean,
        path: String? = null,
    ): Flow<TradingProfile> = withContext(appDispatchers.IO) {
        appDB.transactionWithResult {

            // Insert into DB
            appDB.tradingProfileQueries.insert(
                name = name,
                description = description,
                path = path ?: generateProfilePath(),
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
    ) = withContext(appDispatchers.IO) {

        appDB.tradingProfileQueries.update(
            id = id,
            name = name,
            description = description,
            isTraining = isTraining,
        )
    }

    suspend fun copyProfile(
        id: ProfileId,
        name: (String) -> String,
    ) = withContext(appDispatchers.IO) {

        // Get profile details to copy
        val profile = appDB.tradingProfileQueries.get(id).executeAsOne()

        // Create new entry in DB
        val newProfile = appDB.transactionWithResult {

            // Insert into DB
            appDB.tradingProfileQueries.insert(
                name = name(profile.name),
                description = profile.description,
                path = generateProfilePath(),
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

            // Return copied TradingProfile
            appDB.tradingProfileQueries.get(newProfileId).executeAsOne()
        }

        // Copy associated files
        profile.filesPath.copyToRecursively(
            target = newProfile.filesPath,
            followLinks = false,
            overwrite = false,
        )
    }

    suspend fun deleteProfile(id: ProfileId) = withContext(appDispatchers.IO) {

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
        return appDB.tradingProfileQueries.getDefault().asFlow().mapToOne(appDispatchers.IO)
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

    private fun generateProfilePath(): String = UUID.randomUUID().toString()

    private val TradingProfile.filesPath: Path
        get() = appFilesPath.resolve("Records").resolve(path)
}
