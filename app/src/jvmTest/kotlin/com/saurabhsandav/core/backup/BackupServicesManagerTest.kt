package com.saurabhsandav.core.backup

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.LocalBackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupServicesManagerTest {

    @Test
    fun `Services are empty`() = runTest {

        val settings = MapSettings()
        val prefs = settings.toFlowSettings(Dispatchers.Unconfined)
        val manager = BackupServicesManager(prefs)

        assertTrue { manager.services.first().isEmpty() }
    }

    @Test
    fun `Add Services`() = runTest {

        val settings = MapSettings()
        val prefs = settings.toFlowSettings(Dispatchers.Unconfined)
        val manager = BackupServicesManager(prefs)

        val service1 = LocalBackupService(
            id = BackupService.Id("1"),
            name = "Local 1",
            path = "/Local1",
        )
        val service2 = LocalBackupService(
            id = BackupService.Id("2"),
            name = "Local 2",
            path = "/Local2",
        )

        manager.addService(service1)
        assertEquals(listOf(service1), manager.services.first())

        manager.addService(service2)
        assertEquals(listOf(service1, service2), manager.services.first())
    }

    @Test
    fun `Update Services`() = runTest {

        val settings = MapSettings()
        val prefs = settings.toFlowSettings(Dispatchers.Unconfined)
        val manager = BackupServicesManager(prefs)

        val service1 = LocalBackupService(
            id = BackupService.Id("1"),
            name = "Local 1",
            path = "/Local1",
        )
        val service2 = LocalBackupService(
            id = BackupService.Id("2"),
            name = "Local 2",
            path = "/Local2",
        )

        manager.addService(service1)
        manager.addService(service2)
        assertEquals(listOf(service1, service2), manager.services.first())

        val updatedService1 = LocalBackupService(
            id = BackupService.Id("1"),
            name = "Local 1 (Updated)",
            path = "/Local1",
        )
        manager.addService(updatedService1)
        assertEquals(listOf(updatedService1, service2), manager.services.first())
    }

    @Test
    fun `Delete Services`() = runTest {

        val settings = MapSettings()
        val prefs = settings.toFlowSettings(Dispatchers.Unconfined)
        val manager = BackupServicesManager(prefs)

        val service1 = LocalBackupService(
            id = BackupService.Id("1"),
            name = "Local 1",
            path = "/Local1",
        )
        val service2 = LocalBackupService(
            id = BackupService.Id("2"),
            name = "Local 2",
            path = "/Local2",
        )

        manager.addService(service1)
        manager.addService(service2)
        manager.deleteService(service1.id)

        assertEquals(listOf(service2), manager.services.first())
    }

    @Test
    fun `Get BackupService$Instance`() = runTest {

        val settings = MapSettings()
        val prefs = settings.toFlowSettings(Dispatchers.Unconfined)
        val manager = BackupServicesManager(prefs)

        val service = LocalBackupService(
            id = BackupService.Id("1"),
            name = "Local",
            path = "/Local",
        )

        manager.addService(service)

        assertDoesNotThrow {
            manager.getService(service.id)
        }
    }

    @Test
    fun `Get BackupService$Instance with non-existent service id`() = runTest {

        val settings = MapSettings()
        val prefs = settings.toFlowSettings(Dispatchers.Unconfined)
        val manager = BackupServicesManager(prefs)

        val service = LocalBackupService(
            id = BackupService.Id("1"),
            name = "Local",
            path = "/Local",
        )

        assertFailsWith<IllegalArgumentException> { manager.getService(service.id) }
    }
}
