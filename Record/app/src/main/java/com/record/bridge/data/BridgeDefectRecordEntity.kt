package com.record.bridge.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bridge_defect_record",
    primaryKeys = ["projectId", "componentNo", "defectType", "defectLocation"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class BridgeDefectRecordEntity(
    val projectId: Long,
    val componentNo: String,
    @ColumnInfo(name = "comp_full_code")
    val compFullCode: String,
    val defectType: String,
    val defectLocation: String,
    val quantitativeDesc: String,
    @ColumnInfo(name = "photo_ids")
    val photoIds: String
)
