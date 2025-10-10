package com.saurabhsandav.core.trading

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.FakeAppPaths
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.trading.test.TestBrokerProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradingProfilesTest {

    @Test
    fun `New Profile`() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        assertEquals("Test Name", profile.name)
        assertEquals("Test Desc", profile.description)
        assertTrue(profile.isTraining)
        assertEquals(0, profile.tradeCount)
        assertEquals(0, profile.tradeCountOpen)
    }

    @Test
    fun `New Profile fails on non-unique name`() = runTradingProfilesTest {

        tradingProfiles.createInitialProfile()

        assertFailsWith<IllegalArgumentException>("Profile name (Test Name) is not unique") {
            tradingProfiles.createInitialProfile()
        }
    }

    @Test
    fun `Update Profile with no Record`() = runTradingProfilesTest {

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
        assertTrue { profile.filesPath.notExists() }
        assertTrue { profile.filesSymbolicLinkPath.notExists() }
        assertFalse(updatedProfile.isTraining)
        assertEquals(0, updatedProfile.tradeCount)
        assertEquals(0, updatedProfile.tradeCountOpen)
    }

    @Test
    fun `Update Profile with Record`() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Create Record
        tradingProfiles.getRecord(profile.id)

        // Update
        tradingProfiles.updateProfile(
            id = profile.id,
            name = "New Name",
            description = "New Desc",
            isTraining = false,
        )

        val updatedProfile = tradingProfiles.getProfile(profile.id).first()

        assertEquals("New Name", updatedProfile.name)
        assertTrue { profile.filesPath.exists() }
        // Record Symbolic link updated
        assertTrue { profile.filesSymbolicLinkPath.notExists() }
        assertTrue { updatedProfile.filesSymbolicLinkPath.exists() }
    }

    @Test
    fun `Update Profile fails on non-unique name`() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Create another profile
        tradingProfiles.newProfile(
            name = "New Name",
            description = "",
            isTraining = false,
        )

        assertFailsWith<IllegalArgumentException>("Profile name (Test Name) is not unique") {

            // Update initial profile
            tradingProfiles.updateProfile(
                id = profile.id,
                name = "New Name",
                description = "New Desc",
                isTraining = false,
            )
        }
    }

    @Test
    fun `Copy Profile`() = runTradingProfilesTest {

        // Not possible to create a SQLite DB in a FakeFileSystem. Use a file as a stand-in.
        val testFileText = "Hello! This is a test file"

        fun TradingProfile.testFilePath() = filesPath.resolve("test.txt")

        val profile = tradingProfiles.createInitialProfile()

        // Create Record
        tradingProfiles.getRecord(profile.id)

        // Create test file
        profile.testFilePath().writeText(testFileText, options = arrayOf(StandardOpenOption.CREATE_NEW))

        // Copy
        val newProfile = tradingProfiles.copyProfile(
            copyId = profile.id,
            name = "New Name",
            description = "New Desc",
            isTraining = false,
        ).first()

        assertEquals("New Name", newProfile.name)
        assertEquals("New Desc", newProfile.description)
        assertTrue { newProfile.filesPath.exists() }
        // Record Symbolic link created
        assertTrue { newProfile.filesSymbolicLinkPath.exists() }
        assertEquals(testFileText, newProfile.testFilePath().readText())
        assertFalse(newProfile.isTraining)
        assertEquals(0, newProfile.tradeCount)
        assertEquals(0, newProfile.tradeCountOpen)
    }

    @Test
    fun `Copy Profile fails on non-unique name`() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Create another profile
        tradingProfiles.newProfile(
            name = "New Name",
            description = "",
            isTraining = false,
        )

        assertFailsWith<IllegalArgumentException>("Profile name (Test Name) is not unique") {

            // Copy initial profile
            tradingProfiles.copyProfile(
                copyId = profile.id,
                name = "New Name",
                description = "New Desc",
                isTraining = false,
            )
        }
    }

    @Test
    fun deleteProfile() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Create Record
        tradingProfiles.getRecord(profile.id)

        // Delete
        tradingProfiles.deleteProfile(profile.id)

        assertNull(tradingProfiles.getProfileOrNull(profile.id).first())
        assertFails { tradingProfiles.getRecord(profile.id) }
        // Check Records symbolic link deleted first. `notExists()` returns true if target is deleted first
        assertTrue { profile.filesSymbolicLinkPath.notExists() }
        assertTrue { profile.filesPath.notExists() }
    }

    @Test
    fun getProfile() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        tradingProfiles.getProfile(profile.id).first()

        // Check non-existent
        assertFailsWith<IllegalStateException>(message = "Profile(12) not found") {
            tradingProfiles.getProfile(ProfileId(12)).first()
        }
    }

    @Test
    fun getProfileOrNull() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        assertNotNull(tradingProfiles.getProfileOrNull(profile.id).first())

        // Check non-existent
        assertNull(tradingProfiles.getProfileOrNull(ProfileId(12)).first())
    }

    @Test
    fun getDefault() = runTradingProfilesTest {

        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        assertEquals("Default", defaultProfile.name)
        assertTrue { defaultProfile.description.isEmpty() }
        assertEquals(true, defaultProfile.isTraining)
        assertEquals(0, defaultProfile.tradeCount)
        assertEquals(0, defaultProfile.tradeCountOpen)
    }

    @Test
    fun `Default profile deletion`() = runTradingProfilesTest {

        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        tradingProfiles.deleteProfile(defaultProfile.id)
        assertTrue { tradingProfiles.allProfiles.first().isEmpty() }
        assertTrue { defaultProfile.filesPath.notExists() }
    }

    @Test
    fun `If default was deleted, new profile is set as default`() = runTradingProfilesTest {

        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        // Delete Default profile
        tradingProfiles.deleteProfile(defaultProfile.id)

        // New profile
        val newProfile = tradingProfiles.createInitialProfile()

        assertEquals(newProfile.id, tradingProfiles.getDefaultProfile().first().id)
    }

    @Test
    fun `Default profile auto created if none available`() = runTradingProfilesTest {

        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        // Delete Default profile
        tradingProfiles.deleteProfile(defaultProfile.id)
        assertTrue { tradingProfiles.allProfiles.first().isEmpty() }

        // Check default profile created when none available
        val newDefaultProfile = tradingProfiles.getDefaultProfile().first()

        assertEquals("Default", defaultProfile.name)
        assertTrue { newDefaultProfile.description.isEmpty() }
        assertEquals("DEFAULT", newDefaultProfile.path)
        assertEquals(true, newDefaultProfile.isTraining)
        assertEquals(0, newDefaultProfile.tradeCount)
        assertEquals(0, newDefaultProfile.tradeCountOpen)
    }

    @Test
    fun exists() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        assertTrue { tradingProfiles.exists(profile.id) }

        // Check non-existent
        assertFalse { tradingProfiles.exists(ProfileId(12)) }
    }

    @Test
    fun isProfileNameUnique() = runTradingProfilesTest {

        tradingProfiles.createInitialProfile()

        // Check existing
        assertFalse { tradingProfiles.isProfileNameUnique("Test Name") }

        // Check non-existent
        assertTrue { tradingProfiles.isProfileNameUnique("Random") }
    }

    @Test
    fun getRecord() = runTradingProfilesTest {

        val profile = tradingProfiles.createInitialProfile()

        assertTrue { profile.filesSymbolicLinkPath.notExists() }
        assertTrue { profile.filesPath.notExists() }

        tradingProfiles.getRecord(profile.id)

        assertTrue { profile.filesPath.exists() }
        assertTrue { profile.filesSymbolicLinkPath.exists() }
    }

    private suspend fun TradingProfiles.createInitialProfile(): TradingProfile = newProfile(
        name = "Test Name",
        description = "Test Desc",
        isTraining = true,
    ).first()

    private fun runTradingProfilesTest(block: suspend TradingProfilesTestScope.() -> Unit) = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())

        val driver = JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            schema = AppDB.Schema,
        )

        val appPaths = FakeAppPaths(fakeFileSystem)

        val brokerProvider = TestBrokerProvider
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val tradingProfiles = TradingProfiles(
            coroutineContext = testDispatcher,
            appPaths = appPaths,
            appDB = AppDB(driver),
            brokerProvider = brokerProvider,
            tradingRecordFactory = FakeTradingRecordFactory(
                coroutineContext = testDispatcher,
                brokerProvider = brokerProvider,
            ),
        )

        val scope = object : TradingProfilesTestScope {
            override val appPaths: AppPaths = appPaths
            override val tradingProfiles: TradingProfiles = tradingProfiles
        }

        scope.block()
    }

    private interface TradingProfilesTestScope {

        val appPaths: AppPaths

        val tradingProfiles: TradingProfiles

        val TradingProfile.filesPath: Path
            get() = appPaths.tradingRecordsPath.resolve(path)

        val TradingProfile.filesSymbolicLinkPath: Path
            get() = appPaths.tradingRecordsPath.resolve(name)
    }
}
