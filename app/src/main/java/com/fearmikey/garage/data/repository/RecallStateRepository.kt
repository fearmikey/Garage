package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.local.dao.RecallCampaignStateDao
import com.fearmikey.garage.data.local.entity.RecallCampaignState
import com.fearmikey.garage.data.local.entity.RecallState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecallStateRepository @Inject constructor(
    private val dao: RecallCampaignStateDao
) {
    fun getStatesForVehicle(vehicleId: Long): Flow<List<RecallCampaignState>> =
        dao.getStatesForVehicle(vehicleId)

    suspend fun saveState(vehicleId: Long, campaignNumber: String, state: RecallState) {
        dao.saveState(RecallCampaignState(vehicleId, campaignNumber, state))
    }
}
