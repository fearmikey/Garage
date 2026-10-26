package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fearmikey.garage.data.local.entity.TorqueSpec
import com.fearmikey.garage.data.local.entity.VehicleNote
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM torque_specs WHERE vehicleId = :vehicleId ORDER BY component ASC")
    fun getTorqueSpecs(vehicleId: Long): Flow<List<TorqueSpec>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorqueSpec(spec: TorqueSpec): Long

    @Update
    suspend fun updateTorqueSpec(spec: TorqueSpec)

    @Delete
    suspend fun deleteTorqueSpec(spec: TorqueSpec)

    @Query("SELECT * FROM vehicle_notes WHERE vehicleId = :vehicleId ORDER BY lastUpdated DESC")
    fun getVehicleNotes(vehicleId: Long): Flow<List<VehicleNote>>

    @Query("SELECT * FROM vehicle_notes WHERE id = :noteId")
    suspend fun getVehicleNoteById(noteId: Long): VehicleNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleNote(note: VehicleNote): Long

    @Update
    suspend fun updateVehicleNote(note: VehicleNote)

    @Delete
    suspend fun deleteVehicleNote(note: VehicleNote)
}
