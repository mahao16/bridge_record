package com.record.bridge.data

import androidx.room.Entity

@Entity(
    tableName = "project_record",
    primaryKeys = ["projectId", "componentNo", "defectType", "defectLocation"]
)
data class ProjectRecordEntity(
    val projectId: Long,
    val componentNo: String,
    val defectType: String,
    val defectLocation: String
)

