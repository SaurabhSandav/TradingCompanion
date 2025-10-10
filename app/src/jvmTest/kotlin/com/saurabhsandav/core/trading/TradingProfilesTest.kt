package com.saurabhsandav.core.trading

import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.di.TestGraph
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.flow.first
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
    fun `New Profile`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val profile = testGraph.tradingProfiles.createInitialProfile()

        assertEquals("Test Name", profile.name)
        assertEquals("Test Desc", profile.description)
        assertTrue(profile.isTraining)
        assertEquals(0, profile.tradeCount)
        assertEquals(0, profile.tradeCountOpen)
    }

    @Test
    fun `New Profile fails on non-unique name`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles

        tradingProfiles.createInitialProfile()

        assertFailsWith<IllegalArgumentException>("Profile name (Test Name) is not unique") {
            tradingProfiles.createInitialProfile()
        }
    }

    @Test
    fun `Update Profile with no Record`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
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
        assertTrue { profile.getFilesPath(testGraph).notExists() }
        assertTrue { profile.getFilesSymbolicLinkPath(testGraph).notExists() }
        assertFalse(updatedProfile.isTraining)
        assertEquals(0, updatedProfile.tradeCount)
        assertEquals(0, updatedProfile.tradeCountOpen)
    }

    @Test
    fun `Update Profile with Record`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
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
        assertTrue { profile.getFilesPath(testGraph).exists() }
        // Record Symbolic link updated
        assertTrue { profile.getFilesSymbolicLinkPath(testGraph).notExists() }
        assertTrue { updatedProfile.getFilesSymbolicLinkPath(testGraph).exists() }
    }

    @Test
    fun `Update Profile fails on non-unique name`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
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
    fun `Copy Profile`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles

        // Not possible to create a SQLite DB in a FakeFileSystem. Use a file as a stand-in.
        val testFileText = "Hello! This is a test file"

        fun TradingProfile.testFilePath() = getFilesPath(testGraph).resolve("test.txt")

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
        assertTrue { newProfile.getFilesPath(testGraph).exists() }
        // Record Symbolic link created
        assertTrue { newProfile.getFilesSymbolicLinkPath(testGraph).exists() }
        assertEquals(testFileText, newProfile.testFilePath().readText())
        assertFalse(newProfile.isTraining)
        assertEquals(0, newProfile.tradeCount)
        assertEquals(0, newProfile.tradeCountOpen)
    }

    @Test
    fun `Copy Profile fails on non-unique name`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
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
    fun deleteProfile() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val profile = tradingProfiles.createInitialProfile()

        // Create Record
        tradingProfiles.getRecord(profile.id)

        // Delete
        tradingProfiles.deleteProfile(profile.id)

        assertNull(tradingProfiles.getProfileOrNull(profile.id).first())
        assertFails { tradingProfiles.getRecord(profile.id) }
        // Check Records symbolic link deleted first. `notExists()` returns true if target is deleted first
        assertTrue { profile.getFilesSymbolicLinkPath(testGraph).notExists() }
        assertTrue { profile.getFilesPath(testGraph).notExists() }
    }

    @Test
    fun getProfile() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
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

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        assertNotNull(tradingProfiles.getProfileOrNull(profile.id).first())

        // Check non-existent
        assertNull(tradingProfiles.getProfileOrNull(ProfileId(12)).first())
    }

    @Test
    fun getDefault() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        assertEquals("Default", defaultProfile.name)
        assertTrue { defaultProfile.description.isEmpty() }
        assertEquals(true, defaultProfile.isTraining)
        assertEquals(0, defaultProfile.tradeCount)
        assertEquals(0, defaultProfile.tradeCountOpen)
    }

    @Test
    fun `Default profile deletion`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        tradingProfiles.deleteProfile(defaultProfile.id)
        assertTrue { tradingProfiles.allProfiles.first().isEmpty() }
        assertTrue { defaultProfile.getFilesPath(testGraph).notExists() }
    }

    @Test
    fun `If default was deleted, new profile is set as default`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val defaultProfile = tradingProfiles.getDefaultProfile().first()

        // Delete Default profile
        tradingProfiles.deleteProfile(defaultProfile.id)

        // New profile
        val newProfile = tradingProfiles.createInitialProfile()

        assertEquals(newProfile.id, tradingProfiles.getDefaultProfile().first().id)
    }

    @Test
    fun `Default profile auto created if none available`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
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
    fun exists() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val profile = tradingProfiles.createInitialProfile()

        // Check existing
        assertTrue { tradingProfiles.exists(profile.id) }

        // Check non-existent
        assertFalse { tradingProfiles.exists(ProfileId(12)) }
    }

    @Test
    fun isProfileNameUnique() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        tradingProfiles.createInitialProfile()

        // Check existing
        assertFalse { tradingProfiles.isProfileNameUnique("Test Name") }

        // Check non-existent
        assertTrue { tradingProfiles.isProfileNameUnique("Random") }
    }

    @Test
    fun getRecord() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)
        val tradingProfiles = testGraph.tradingProfiles
        val profile = tradingProfiles.createInitialProfile()

        assertTrue { profile.getFilesSymbolicLinkPath(testGraph).notExists() }
        assertTrue { profile.getFilesPath(testGraph).notExists() }

        tradingProfiles.getRecord(profile.id)

        assertTrue { profile.getFilesPath(testGraph).exists() }
        assertTrue { profile.getFilesSymbolicLinkPath(testGraph).exists() }
    }

    private suspend fun TradingProfiles.createInitialProfile(): TradingProfile = newProfile(
        name = "Test Name",
        description = "Test Desc",
        isTraining = true,
    ).first()

    private fun TradingProfile.getFilesPath(testGraph: TestGraph): Path {
        return testGraph.appPaths.tradingRecordsPath.resolve(path)
    }

    private fun TradingProfile.getFilesSymbolicLinkPath(testGraph: TestGraph): Path {
        return testGraph.appPaths.tradingRecordsPath.resolve(name)
    }
}
