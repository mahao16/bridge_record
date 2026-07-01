package com.record.bridge.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary",
    indices = [Index(value = ["category", "label"], unique = true)]
)
data class DictionaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val label: String,
    val remark: String,
    val isDefault: Boolean,
    val isActive: Boolean
)

