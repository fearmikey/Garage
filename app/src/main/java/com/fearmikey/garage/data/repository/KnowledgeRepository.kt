package com.fearmikey.garage.data.repository

import androidx.room.withTransaction
import com.fearmikey.garage.data.local.GarageDatabase
import com.fearmikey.garage.data.local.dao.KnowledgeDao
import com.fearmikey.garage.data.local.dao.LubeLoggerPendingDeleteDao
import com.fearmikey.garage.data.local.entity.LubeLoggerPendingDelete
import com.fearmikey.garage.data.local.entity.LubeLoggerRecordType
import com.fearmikey.garage.data.local.entity.TorqueSpec
import com.fearmikey.garage.data.local.entity.VehicleNote
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeRepository @Inject constructor(
    private val dao: KnowledgeDao,
    private val pendingDeleteDao: LubeLoggerPendingDeleteDao,
    private val database: GarageDatabase,
) {
    fun getTorqueSpecs(vehicleId: Long): Flow<List<TorqueSpec>> = dao.getTorqueSpecs(vehicleId)
    
    suspend fun insertTorqueSpec(spec: TorqueSpec) = dao.insertTorqueSpec(spec)
    
    suspend fun updateTorqueSpec(spec: TorqueSpec) = dao.updateTorqueSpec(spec)
    
    suspend fun deleteTorqueSpec(spec: TorqueSpec) = dao.deleteTorqueSpec(spec)

    fun getVehicleNotes(vehicleId: Long): Flow<List<VehicleNote>> = dao.getVehicleNotes(vehicleId)
    
    suspend fun getVehicleNoteById(noteId: Long): VehicleNote? = dao.getVehicleNoteById(noteId)
    
    suspend fun insertVehicleNote(note: VehicleNote): Long = dao.insertVehicleNote(note)
    
    suspend fun updateVehicleNote(note: VehicleNote) = dao.updateVehicleNote(note)

    /** Deletes a note and, if it was synced, queues the matching LubeLogger delete. */
    suspend fun deleteVehicleNote(note: VehicleNote) {
        database.withTransaction {
            note.lubeLoggerId?.let { remoteId ->
                pendingDeleteDao.insert(
                    LubeLoggerPendingDelete(
                        type = LubeLoggerRecordType.NOTE,
                        lubeLoggerId = remoteId,
                        lubeLoggerVehicleId = note.vehicleId,
                    ),
                )
            }
            dao.deleteVehicleNote(note)
        }
    }

    /** Inserts or replaces a note pulled from (or linked to) LubeLogger. */
    suspend fun saveVehicleNoteFromSync(note: VehicleNote): Long = dao.insertVehicleNote(note)

    /** Deletes a note that was already deleted in LubeLogger, without queuing a server delete. */
    suspend fun deleteVehicleNoteFromSync(note: VehicleNote) = dao.deleteVehicleNote(note)
}
