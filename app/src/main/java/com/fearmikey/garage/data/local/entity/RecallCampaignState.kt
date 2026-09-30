package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class RecallState {
    OPEN,
    SERVICED,
    DOES_NOT_AFFECT
}

@Entity(
    tableName = "recall_campaign_states",
    primaryKeys = ["vehicleId", "campaignNumber"],
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vehicleId"])]
)
data class RecallCampaignState(
    val vehicleId: Long,
    val campaignNumber: String,
    val state: RecallState
)
