package com.record.bridge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_draft")
data class LocationDraftEntity(
    @PrimaryKey
    val id: Int = 1,
    val position: String
)

