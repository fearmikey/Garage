package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.KnowledgeDao
import com.fearmikey.garage.data.local.entity.TorqueSpec
import com.fearmikey.garage.data.local.entity.VehicleNote
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeRepository @Inject constructor(
    private val dao: KnowledgeDao
) {
    fun getTorqueSpecs(vehicleId: Long): Flow<List<TorqueSpec>> = dao.getTorqueSpecs(vehicleId)
    
    suspend fun insertTorqueSpec(spec: TorqueSpec) = dao.insertTorqueSpec(spec)
    
    suspend fun updateTorqueSpec(spec: TorqueSpec) = dao.updateTorqueSpec(spec)
    
    suspend fun deleteTorqueSpec(spec: TorqueSpec) = dao.deleteTorqueSpec(spec)

    fun getVehicleNotes(vehicleId: Long): Flow<List<VehicleNote>> = dao.getVehicleNotes(vehicleId)
    
    suspend fun getVehicleNoteById(noteId: Long): VehicleNote? = dao.getVehicleNoteById(noteId)
    
    suspend fun insertVehicleNote(note: VehicleNote): Long = dao.insertVehicleNote(note)
    
    suspend fun updateVehicleNote(note: VehicleNote) = dao.updateVehicleNote(note)
    
    suspend fun deleteVehicleNote(note: VehicleNote) = dao.deleteVehicleNote(note)
}
