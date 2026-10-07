package com.fearmikey.garage.data.remote.lubelogger

import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete

/**
 * Generic sync engine for LubeLogger records.
 */
suspend fun <LocalT, RemoteT> syncLubeLoggerRecords(
    localRecords: List<LocalT>,
    remoteRecords: List<RemoteT>,
    pendingDeletes: List<LubeLoggerPendingDelete>,
    getLubeLoggerId: (LocalT) -> Int?,
    getRemoteId: (RemoteT) -> Int,
    getStoredHash: (LocalT) -> String?,
    getLocalFingerprint: (LocalT) -> String,
    getRemoteFingerprint: (RemoteT) -> String,
    toDto: (LocalT) -> RemoteT,
    toLocal: (RemoteT, LocalT?) -> LocalT?,
    isSameReadingAs: (LocalT, RemoteT) -> Boolean,
    addLocal: suspend (LocalT) -> Unit,
    updateLocal: suspend (LocalT) -> Unit,
    deleteLocal: suspend (LocalT) -> Unit,
    addRemote: suspend (RemoteT) -> Int?,
    updateRemote: suspend (RemoteT) -> Boolean,
    deleteRemote: suspend (Int) -> Boolean,
    onPendingDeleteProcessed: suspend (LubeLoggerPendingDelete) -> Unit,
    withSyncState: (LocalT, Int, String) -> LocalT,
    /** Called with (skipped, linked) when the 50% safety limit blocks server-side deletes. */
    onDeletesSkipped: (Int, Int) -> Unit = { _, _ -> },
    /** Called after a local record is successfully created on the remote server. */
    onRemoteAdded: suspend (LocalT, Int) -> Unit = { _, _ -> },
) {
    // 1. Process pending deletes. The server list was fetched before these ran, so drop those
    //    records from it; otherwise they'd be pulled back into Garage as "new".
    val deletedInGarage = pendingDeletes.map { it.lubeLoggerId }.toSet()
    for (delete in pendingDeletes) {
        if (deleteRemote(delete.lubeLoggerId)) {
            onPendingDeleteProcessed(delete)
        }
    }
    @Suppress("NAME_SHADOWING")
    val remoteRecords = remoteRecords.filter { getRemoteId(it) !in deletedInGarage }

    val linkedLocalRecords = localRecords.filter { getLubeLoggerId(it) != null }.toMutableList()
    val remoteIds = remoteRecords.map { getRemoteId(it) }.toSet()

    // 2. Identify local records that were deleted on the server
    val localRecordsToDelete = linkedLocalRecords.filter { local ->
        val llId = getLubeLoggerId(local)!!
        llId !in remoteIds && pendingDeletes.none { it.lubeLoggerId == llId }
    }

    if (localRecordsToDelete.isNotEmpty()) {
        if (linkedLocalRecords.size >= 2 && localRecordsToDelete.size > linkedLocalRecords.size / 2) {
            onDeletesSkipped(localRecordsToDelete.size, linkedLocalRecords.size)
        } else {
            for (record in localRecordsToDelete) {
                deleteLocal(record)
                linkedLocalRecords.remove(record)
            }
        }
    }

    val updatedUnlinkedLocals = mutableSetOf<LocalT>()

    // 3. Process remote records (Update existing, link matches, or pull new)
    for (remoteRecord in remoteRecords) {
        val remoteId = getRemoteId(remoteRecord)
        val remoteFingerprint = getRemoteFingerprint(remoteRecord)
        
        val localRecord = linkedLocalRecords.find { getLubeLoggerId(it) == remoteId }

        if (localRecord != null) {
            val localFingerprint = getLocalFingerprint(localRecord)
            val storedHash = getStoredHash(localRecord)

            val localChanged = storedHash != null && localFingerprint != storedHash
            val remoteChanged = storedHash != null && remoteFingerprint != storedHash

            if (localChanged && !remoteChanged) {
                // Garage changed -> Push
                if (updateRemote(toDto(localRecord))) {
                    updateLocal(withSyncState(localRecord, remoteId, localFingerprint))
                }
            } else if (remoteChanged && !localChanged) {
                // Server changed -> Pull
                val updatedLocal = toLocal(remoteRecord, localRecord)
                if (updatedLocal != null) {
                    updateLocal(withSyncState(updatedLocal, remoteId, remoteFingerprint))
                }
            } else if (localChanged && localFingerprint != remoteFingerprint) {
                // Both changed -> Garage wins
                if (updateRemote(toDto(localRecord))) {
                    updateLocal(withSyncState(localRecord, remoteId, localFingerprint))
                }
            } else if (storedHash == null && localFingerprint != remoteFingerprint) {
                // Linked before fingerprints existed and the two sides differ: no history to tell
                // which side changed, so Garage wins (also rewrites legacy-format server records).
                if (updateRemote(toDto(localRecord))) {
                    updateLocal(withSyncState(localRecord, remoteId, localFingerprint))
                }
            } else if (storedHash != localFingerprint || storedHash != remoteFingerprint) {
                // First sync with identical data, or both sides converged -> just record the hash
                updateLocal(withSyncState(localRecord, remoteId, localFingerprint))
            }
        } else {
            // Unlinked?
            val unlinkedLocal = localRecords.find { 
                getLubeLoggerId(it) == null && isSameReadingAs(it, remoteRecord) && it !in updatedUnlinkedLocals 
            }
            if (unlinkedLocal != null) {
                // Link them. Server record details override local (e.g. costs).
                val updatedLocal = toLocal(remoteRecord, unlinkedLocal) ?: unlinkedLocal
                // But we preserve the local ID so we UPDATE the local record rather than INSERT
                updateLocal(withSyncState(updatedLocal, remoteId, remoteFingerprint))
                updatedUnlinkedLocals.add(unlinkedLocal)
            } else {
                // Completely new on server -> Pull
                val newLocal = toLocal(remoteRecord, null)
                if (newLocal != null) {
                    addLocal(withSyncState(newLocal, remoteId, remoteFingerprint))
                }
            }
        }
    }

    // 4. Push new unlinked local records
    val newRecords = localRecords.filter { getLubeLoggerId(it) == null && it !in updatedUnlinkedLocals }
    for (record in newRecords) {
        val localFingerprint = getLocalFingerprint(record)
        val newRemoteId = addRemote(toDto(record))
        if (newRemoteId != null) {
            onRemoteAdded(record, newRemoteId)
            updateLocal(withSyncState(record, newRemoteId, localFingerprint))
        }
    }
}
