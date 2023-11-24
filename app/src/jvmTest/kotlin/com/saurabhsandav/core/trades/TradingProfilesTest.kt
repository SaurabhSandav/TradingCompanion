package com.saurabhsandav.core.trades

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.FakeAppPaths
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.ProfileIdColumnAdapter
import com.saurabhsandav.core.utils.DbUrlProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import kotlin.test.*

class TradingProfilesTest {

    @Test
    fun newProfile() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        assertEquals("Test Name", profile.name)
        assertEquals("Test Desc", profile.description)
        assertTrue(profile.isTraining)
        assertEquals(0, profile.tradeCount)
        assertEquals(0, profile.tradeCountOpen)
    }

    @Test
    fun updateProfile() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        // Update
        tradingProfiles.updateProfile(
            id = profile.id,
            name = "New Name",
            description = "New Desc",
            isTraining = false,
        )

        val updatedProfile = tradingProfiles.getProfile(profile.id).first()

        assertEquals("New Name", updatedProfile.name)
        assertEquals("New Desc", updatedProfile.description)
        assertFalse(updatedProfile.isTraining)
        assertEquals(0, updatedProfile.tradeCount)
        assertEquals(0, updatedProfile.tradeCountOpen)
    }

    @Test
    fun copyProfile() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        // Copy
        val newProfile = tradingProfiles.copyProfile(
            id = profile.id,
            name = { "Duplicate of $it" },
        ).first()

        assertEquals("Duplicate of Test Name", newProfile.name)
        assertEquals("Test Desc", newProfile.description)
        assertTrue(newProfile.isTraining)
        assertEquals(0, newProfile.tradeCount)
        assertEquals(0, newProfile.tradeCountOpen)
    }

    @Test
    fun deleteProfile() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        // Delete
        tradingProfiles.deleteProfile(profile.id)

        assertNull(tradingProfiles.getProfileOrNull(profile.id).first())
    }

    @Test
    fun getProfile() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        tradingProfiles.getProfile(profile.id).first()

        // Check non-existent
        assertFailsWith<IllegalStateException>(message = "Profile(12) not found") {
            tradingProfiles.getProfile(ProfileId(12)).first()
        }
    }

    @Test
    fun getProfileOrNull() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        assertNotNull(tradingProfiles.getProfileOrNull(profile.id).first())

        // Check non-existent
        assertNull(tradingProfiles.getProfileOrNull(ProfileId(12)).first())
    }

    @Test
    fun getDefault() = runTest {

        val tradingProfiles = createTradingProfiles()

        // Check default profile exists as initial state
        val initialDefaultProfile = tradingProfiles.getDefaultProfile().first()
        assertEquals("Default", initialDefaultProfile.name)
        assertTrue { initialDefaultProfile.description.isEmpty() }
        assertEquals("DEFAULT", initialDefaultProfile.path)
        assertEquals(true, initialDefaultProfile.isTraining)
        assertEquals(0, initialDefaultProfile.tradeCount)
        assertEquals(0, initialDefaultProfile.tradeCountOpen)

        // Check default profile deletion
        tradingProfiles.deleteProfile(initialDefaultProfile.id)
        assertTrue { tradingProfiles.allProfiles.first().isEmpty() }

        // New profile
        val newProfile = tradingProfiles.createInitialProfile()

        // Check new profile is considered default profile
        assertEquals(newProfile.id, tradingProfiles.getDefaultProfile().first().id)

        // Delete new profile
        tradingProfiles.deleteProfile(newProfile.id)
        assertTrue { tradingProfiles.allProfiles.first().isEmpty() }

        // Check default profile created when none available
        val newDefaultProfile = tradingProfiles.getDefaultProfile().first()
        assertTrue { newDefaultProfile.description.isEmpty() }
        assertEquals("DEFAULT", newDefaultProfile.path)
        assertEquals(true, newDefaultProfile.isTraining)
        assertEquals(0, newDefaultProfile.tradeCount)
        assertEquals(0, newDefaultProfile.tradeCountOpen)
    }

    @Test
    fun exists() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        assertTrue { tradingProfiles.exists(profile.id) }

        // Check non-existent
        assertFalse { tradingProfiles.exists(ProfileId(12)) }
    }

    @Test
    fun isProfileNameUnique() = runTest {

        val tradingProfiles = createTradingProfiles()

        tradingProfiles.createInitialProfile()

        // Check existing
        assertFalse { tradingProfiles.isProfileNameUnique("Test Name") }

        // Check non-existent
        assertTrue { tradingProfiles.isProfileNameUnique("Random") }
    }

    @Test
    fun getRecord() = runTest {

        val tradingProfiles = createTradingProfiles()
        val profile = tradingProfiles.createInitialProfile()

        tradingProfiles.getRecord(profile.id)
    }

    private suspend fun TradingProfiles.createInitialProfile(): TradingProfile = newProfile(
        name = "Test Name",
        description = "Test Desc",
        isTraining = true,
    ).first()

    private fun TestScope.createTradingProfiles(): TradingProfiles {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())

        val driver = JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            schema = AppDB.Schema,
        )

        val dbUrlProvider = object : DbUrlProvider {
            override fun getAppDbUrl(): String = JdbcSqliteDriver.IN_MEMORY
            override fun getCandlesDbUrl(): String = JdbcSqliteDriver.IN_MEMORY
            override fun getTradingRecordDbUrl(path: Path): String = JdbcSqliteDriver.IN_MEMORY
        }

        return TradingProfiles(
            appDispatchers = FakeAppDispatchers(this),
            appPaths = FakeAppPaths(fakeFileSystem),
            dbUrlProvider = dbUrlProvider,
            appDB = AppDB(
                driver = driver,
                TradingProfileAdapter = TradingProfile.Adapter(
                    idAdapter = ProfileIdColumnAdapter,
                    tradeCountAdapter = IntColumnAdapter,
                    tradeCountOpenAdapter = IntColumnAdapter,
                ),
            ),
        )
    }
}
