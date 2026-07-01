package com.record.bridge.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SiteLogEntity): Long

    @Delete
    suspend fun delete(entity: SiteLogEntity)

    @Query("DELETE FROM site_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM site_log WHERE recordId = :recordId")
    suspend fun deleteByRecordId(recordId: String)

    @Query("DELETE FROM site_log WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Long)

    @Query("SELECT * FROM site_log WHERE id = :id LIMIT 1")
    suspend fun getOne(id: Long): SiteLogEntity?

    @Query("SELECT * FROM site_log WHERE recordId = :recordId ORDER BY timestamp ASC, id ASC")
    suspend fun listByRecordId(recordId: String): List<SiteLogEntity>

    @Query("SELECT * FROM site_log WHERE projectId = :projectId ORDER BY timestamp ASC, id ASC")
    suspend fun listByProject(projectId: Long): List<SiteLogEntity>

    @Query("SELECT * FROM site_log WHERE id IN (:ids) ORDER BY timestamp ASC, id ASC")
    suspend fun listByIds(ids: List<Long>): List<SiteLogEntity>

    @Query("SELECT * FROM site_log WHERE recordId IN (:recordIds) ORDER BY timestamp ASC, id ASC")
    suspend fun listByRecordIds(recordIds: List<String>): List<SiteLogEntity>

    @Query(
        """
        SELECT * FROM site_log
        WHERE projectId = :projectId
          AND logType = :logType
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun observeByProjectAndType(projectId: Long, logType: String): Flow<List<SiteLogEntity>>

    @Query(
        """
        SELECT * FROM site_log
        WHERE projectId = :projectId
          AND logType = :logType
        ORDER BY timestamp ASC, id ASC
        """
    )
    suspend fun getLogsByProjectAndType(projectId: Long, logType: String): List<SiteLogEntity>

    @Query(
        """
        SELECT * FROM site_log
        WHERE logType = :logType
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun observeByType(logType: String): Flow<List<SiteLogEntity>>

    @Query(
        """
        SELECT * FROM site_log
        WHERE logType = :logType
        ORDER BY timestamp ASC, id ASC
        """
    )
    suspend fun listByType(logType: String): List<SiteLogEntity>

    @Query("UPDATE site_log SET description = :newDesc WHERE id = :id")
    suspend fun updateDescription(id: Long, newDesc: String)

    @Query("UPDATE site_log SET description = :newDesc WHERE recordId = :recordId")
    suspend fun updateDescriptionByRecordId(recordId: String, newDesc: String)
}

