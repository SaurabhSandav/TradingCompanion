package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    private val appPrefs: FlowSettings,
    private val candleRepo: CandleRepository,
) {

    private val records = mutableMapOf<Long, TradingRecord>()
    private val recordBuilderMutex = Mutex()

    val allProfiles = appDB.tradingProfileQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    val currentProfile = appPrefs.getLongOrNullFlow(PrefKeys.CurrentTradingProfile).flatMapLatest { profileId ->

        val newProfileId = when (profileId) {
            // Select first profile from stored profiles
            null -> allProfiles.first().first().id

            // Current profile
            else -> profileId
        }

        appDB.tradingProfileQueries.get(newProfileId).asFlow().mapToOne(Dispatchers.IO)
    }.distinctUntilChanged()

    val currentRecord = currentProfile.map { getRecord(it.id) }

    fun getProfile(id: Long): Flow<TradingProfile> {
        return allProfiles.map { profiles -> profiles.find { it.id == id } ?: error("Profile($id) not found") }
    }

    fun getProfileOrNull(id: Long): Flow<TradingProfile?> {
        return allProfiles.map { profiles -> profiles.find { it.id == id } }
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
            val id = appDB.appDBUtilsQueries.lastInsertedRowId().executeAsOne()

            // Return TradingProfile
            appDB.tradingProfileQueries.get(id).asFlow().mapToOne(Dispatchers.IO)
        }
    }

    suspend fun updateProfile(
        id: Long,
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
        id: Long,
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

    suspend fun deleteProfile(id: Long) = withContext(Dispatchers.IO) {

        // Remove record
        records.remove(id)

        // Delete from DB
        val profile = appDB.tradingProfileQueries.get(id).executeAsOne()
        appDB.tradingProfileQueries.delete(id)

        // Delete associated files
        profile.filesPath.toFile().deleteRecursively()
    }

    suspend fun setCurrentProfile(id: Long) = withContext(Dispatchers.IO) {
        appPrefs.putLong(PrefKeys.CurrentTradingProfile, id)
    }

    suspend fun getRecord(id: Long): TradingRecord = recordBuilderMutex.withLock {

        records.getOrPut(id) {

            withContext(Dispatchers.IO) {

                val profile = appDB.tradingProfileQueries.get(id).executeAsOne()
                val profileFilesPath = profile.filesPath.createDirectories()

                TradingRecord(
                    recordPath = profileFilesPath.toString(),
                    candleRepo = candleRepo,
                )
            }
        }
    }

    private val TradingProfile.filesPath: Path
        get() = Path("$appFilesPath/Records/$path")
}
