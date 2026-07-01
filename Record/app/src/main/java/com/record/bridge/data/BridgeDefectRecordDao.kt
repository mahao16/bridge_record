package com.record.bridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BridgeDefectRecordDao {
    @Query(
        """
        SELECT * FROM bridge_defect_record
        ORDER BY projectId ASC, componentNo ASC, defectType ASC, defectLocation ASC
        """
    )
    fun observeAll(): Flow<List<BridgeDefectRecordEntity>>

    @Query(
        """
        SELECT * FROM bridge_defect_record
        WHERE projectId = :projectId
        ORDER BY componentNo ASC, defectType ASC, defectLocation ASC
        """
    )
    fun observeByProject(projectId: Long): Flow<List<BridgeDefectRecordEntity>>

    @Query(
        """
        SELECT * FROM bridge_defect_record
        WHERE projectId = :projectId
        ORDER BY componentNo ASC, defectType ASC, defectLocation ASC
        """
    )
    suspend fun listByProject(projectId: Long): List<BridgeDefectRecordEntity>

    @Query(
        """
        SELECT * FROM bridge_defect_record
        WHERE projectId = :projectId
          AND componentNo = :componentNo
          AND defectType = :defectType
          AND defectLocation = :defectLocation
        LIMIT 1
        """
    )
    suspend fun getOne(
        projectId: Long,
        componentNo: String,
        defectType: String,
        defectLocation: String
    ): BridgeDefectRecordEntity?

    @Query(
        """
        DELETE FROM bridge_defect_record
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

    @Query("DELETE FROM bridge_defect_record WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BridgeDefectRecordEntity)
}
