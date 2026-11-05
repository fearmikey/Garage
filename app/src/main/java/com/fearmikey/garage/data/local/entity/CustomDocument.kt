package com.fearmikey.garage.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A free-form vehicle document the user names themselves (extended warranty, roadside
 * assistance, title, loan/lease contract, club membership...), for anything the fixed
 * sections of [VehicleRegistrationInsurance] don't cover.
 *
 * [expiration] participates in document renewal reminders just like the built-in sections.
 */
@Entity(
    tableName = "custom_documents",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicleId")],
)
data class CustomDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    /** Policy / contract / account / reference number. */
    val referenceNumber: String? = null,
    val expiration: Long? = null,
    val cost: Double? = null,
    val notes: String? = null,
    /** Attached photo filename under ImageStorageManager's images dir. */
    val imageUri: String? = null,
)
