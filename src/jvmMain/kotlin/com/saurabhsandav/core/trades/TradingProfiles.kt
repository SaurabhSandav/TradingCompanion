package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal class TradingProfiles(
    private val appFilesPath: String,
    private val appDB: AppDB,
) {

    private val records = mutableMapOf<ProfileId, TradingRecord>()
    private val recordBuilderMutex = Mutex()

    val allProfiles: Flow<List<TradingProfile>> =
        appDB.tradingProfileQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getProfile(id: ProfileId): Flow<TradingProfile> {
        return getProfileOrNull(id).map { it ?: error("Profile($id) not found") }
    }

    fun getProfileOrNull(id: ProfileId): Flow<TradingProfile?> {
        return appDB.tradingProfileQueries.get(id).asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    fun getDefaultProfile(): Flow<TradingProfile> {
        return appDB.tradingProfileQueries.getDefault().asFlow().mapToOne(Dispatchers.IO)
    }

    suspend fun newProfile(
        name: String,
        description: String,
        isTraining: Boolean,
        path: String? = null,
    ): Flow<TradingProfile> = withContext(Dispatchers.IO) {
        appDB.transactionWithResult {

            // Insert into DB
            appDB.tradingProfileQueries.insert(
                name = name,
                description = description,
                path = path ?: UUID.randomUUID().toString(),
                isTraining = isTraining,
            )

            // Get id of last inserted row
            val id = appDB.appDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::ProfileId)

            // Return TradingProfile
            appDB.tradingProfileQueries.get(id).asFlow().mapToOne(Dispatchers.IO)
        }
    }

    suspend fun exists(id: ProfileId): Boolean = withContext(Dispatchers.IO) {
        return@withContext appDB.tradingProfileQueries.exists(id).executeAsOne()
    }

    suspend fun updateProfile(
        id: ProfileId,
        name: String,
        description: String,
        isTraining: Boolean,
    ) = withContext(Dispatchers.IO) {

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
    ) = withContext(Dispatchers.IO) {

        // Get profile details to copy
        val profile = appDB.tradingProfileQueries.get(id).executeAsOne()

        // Create new entry in DB
        val newProfile = newProfile(
            name = name(profile.name),
            description = profile.description,
            isTraining = profile.isTraining,
        ).first()

        // Copy associated files
        profile.filesPath.toFile().copyRecursively(newProfile.filesPath.toFile())
    }

    suspend fun deleteProfile(id: ProfileId) = withContext(Dispatchers.IO) {

        // Remove record
        records.remove(id)

        // Delete from DB
        val profile = appDB.tradingProfileQueries.get(id).executeAsOne()
        appDB.tradingProfileQueries.delete(id)

        // Delete associated files
        profile.filesPath.toFile().deleteRecursively()
    }

    suspend fun isProfileNameUnique(
        name: String,
        ignoreProfileId: ProfileId? = null,
    ): Boolean = withContext(Dispatchers.IO) {
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

            withContext(Dispatchers.IO) {

                val profile = appDB.tradingProfileQueries.get(id).executeAsOne()
                val profileFilesPath = profile.filesPath.createDirectories()

                TradingRecord(
                    recordPath = profileFilesPath.toString(),
                )
            }
        }
    }

    private val TradingProfile.filesPath: Path
        get() = Path("$appFilesPath/Records/$path")
}
