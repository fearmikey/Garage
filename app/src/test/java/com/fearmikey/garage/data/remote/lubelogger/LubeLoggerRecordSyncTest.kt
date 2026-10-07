package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the three-way merge in [syncLubeLoggerRecords] with a minimal record model, one test
 * per row of the "since last sync" table in the implementation plan.
 */
class LubeLoggerRecordSyncTest {

    private data class Local(val localId: Long, val value: String, val llId: Int? = null, val hash: String? = null, val mileage: Int = 0)
    private data class Remote(val id: Int, val value: String, val mileage: Int = 0)

    /** An in-memory "Garage" and "LubeLogger" that the engine edits. */
    private class World(local: List<Local>, remote: List<Remote>) {
        val local = local.toMutableList()
        val remote = remote.toMutableList()
        val pendingDeletes = mutableListOf<LubeLoggerPendingDelete>()
        private var nextRemoteId = 1000
        private var nextLocalId = 1000L

        suspend fun sync() = syncLubeLoggerRecords(
            localRecords = local.toList(),
            remoteRecords = remote.toList(),
            pendingDeletes = pendingDeletes.toList(),
            getLubeLoggerId = { it.llId },
            getRemoteId = { it.id },
            getStoredHash = { it.hash },
            getLocalFingerprint = { it.value },
            getRemoteFingerprint = { it.value },
            toDto = { Remote(it.llId ?: 0, it.value, it.mileage) },
            toLocal = { dto, existing -> Local(existing?.localId ?: 0, dto.value, dto.id, mileage = dto.mileage) },
            isSameReadingAs = { l, r -> l.mileage == r.mileage && l.mileage != 0 },
            addLocal = { local += it.copy(localId = nextLocalId++) },
            updateLocal = { updated -> local.replaceAll { if (it.localId == updated.localId) updated else it } },
            deleteLocal = { deleted -> local.removeAll { it.localId == deleted.localId } },
            addRemote = { dto -> nextRemoteId++.also { remote += dto.copy(id = it) } },
            updateRemote = { dto -> remote.replaceAll { if (it.id == dto.id) dto else it }; true },
            deleteRemote = { id -> remote.removeAll { it.id == id }; true },
            onPendingDeleteProcessed = { pendingDeletes.remove(it) },
            withSyncState = { l, id, hash -> l.copy(llId = id, hash = hash) },
        )
    }

    private fun synced(localId: Long, llId: Int, value: String) = Local(localId, value, llId, hash = value)

    @Test
    fun `only Garage changed - uploads Garage version`() = runTest {
        val world = World(listOf(synced(1, 10, "old").copy(value = "new")), listOf(Remote(10, "old")))
        world.sync()
        assertEquals("new", world.remote.single().value)
        assertEquals("new", world.local.single().hash)
    }

    @Test
    fun `only LubeLogger changed - downloads server version`() = runTest {
        val world = World(listOf(synced(1, 10, "old")), listOf(Remote(10, "server")))
        world.sync()
        assertEquals("server", world.local.single().value)
        assertEquals(1L, world.local.single().localId)
    }

    @Test
    fun `both changed - Garage wins`() = runTest {
        val world = World(listOf(synced(1, 10, "old").copy(value = "garage")), listOf(Remote(10, "server")))
        world.sync()
        assertEquals("garage", world.remote.single().value)
        assertEquals("garage", world.local.single().value)
    }

    @Test
    fun `first sync of a pre-linked record that differs - Garage wins and is remembered`() = runTest {
        val world = World(listOf(Local(1, "garage", llId = 10, hash = null)), listOf(Remote(10, "server")))
        world.sync()
        assertEquals("garage", world.remote.single().value)
        world.sync() // must stay stable, not pull the old server value back
        assertEquals("garage", world.local.single().value)
        assertEquals("garage", world.local.single().hash)
    }

    @Test
    fun `deleted in LubeLogger - deleted in Garage`() = runTest {
        val world = World(
            listOf(synced(1, 10, "a"), synced(2, 11, "b"), synced(3, 12, "c")),
            listOf(Remote(10, "a"), Remote(11, "b")),
        )
        world.sync()
        assertEquals(listOf(1L, 2L), world.local.map { it.localId })
    }

    @Test
    fun `deleted in Garage - deleted in LubeLogger and not pulled back`() = runTest {
        val world = World(listOf(synced(2, 11, "b")), listOf(Remote(10, "a"), Remote(11, "b")))
        world.pendingDeletes += LubeLoggerPendingDelete(id = 1, type = LubeLoggerRecordType.FUEL, lubeLoggerId = 10, lubeLoggerVehicleId = 1)
        world.sync()
        assertEquals(listOf(11), world.remote.map { it.id })
        assertEquals(listOf(2L), world.local.map { it.localId })
        assertTrue(world.pendingDeletes.isEmpty())
    }

    @Test
    fun `safety limit skips deletes that would remove more than half`() = runTest {
        val world = World(
            listOf(synced(1, 10, "a"), synced(2, 11, "b"), synced(3, 12, "c")),
            listOf(Remote(10, "a")),
        )
        world.sync()
        assertEquals(3, world.local.size)
    }

    @Test
    fun `new records are added on both sides`() = runTest {
        val world = World(listOf(Local(1, "garage-only", mileage = 5)), listOf(Remote(20, "server-only", mileage = 9)))
        world.sync()
        assertEquals(setOf("garage-only", "server-only"), world.remote.map { it.value }.toSet())
        assertEquals(setOf("garage-only", "server-only"), world.local.map { it.value }.toSet())
        assertTrue(world.local.all { it.llId != null && it.hash != null })
    }

    @Test
    fun `unlinked records with the same reading are linked instead of duplicated`() = runTest {
        val world = World(listOf(Local(1, "x", mileage = 500)), listOf(Remote(30, "x", mileage = 500)))
        world.sync()
        assertEquals(1, world.local.size)
        assertEquals(1, world.remote.size)
        assertEquals(30, world.local.single().llId)
    }
}
