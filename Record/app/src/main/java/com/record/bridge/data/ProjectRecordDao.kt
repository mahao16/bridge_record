package com.record.bridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProjectRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectRecordEntity)

    @Query("DELETE FROM project_record WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Query(
        """
        DELETE FROM project_record
        WHERE projectId = :projectId
          AND componentNo = :componentNo
          AND defectType = :defectType
          AND defectLocation = :defectLocation
        """
    )
    suspend fun deleteOne(
        projectId: Long,
        componentNo: String,
        defectType: String,
        defectLocation: String
    )

    @Query(
        """
        SELECT COUNT(1) FROM project_record
        WHERE componentNo = :componentNo
          AND defectType = :defectType
          AND defectLocation = :defectLocation
        """
    )
    suspend fun countRefs(
        componentNo: String,
        defectType: String,
        defectLocation: String
    ): Int
}

