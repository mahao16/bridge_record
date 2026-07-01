package com.record.bridge.data

data class ProjectWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val recordCount: Int
)

