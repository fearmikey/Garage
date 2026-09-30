package com.fearmikey.garage.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fearmikey.garage.data.local.entity.RecallCampaignState
import kotlinx.coroutines.flow.Flow

@Dao
interface RecallCampaignStateDao {
    @Query("SELECT * FROM recall_campaign_states WHERE vehicleId = :vehicleId")
    fun getStatesForVehicle(vehicleId: Long): Flow<List<RecallCampaignState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: RecallCampaignState)
}
