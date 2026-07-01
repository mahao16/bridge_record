package com.record.bridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query(
        """
        SELECT
            p.id AS id,
            p.name AS name,
            p.createdAt AS createdAt,
            COUNT(r.componentNo) AS recordCount
        FROM project p
        LEFT JOIN bridge_defect_record r
            ON r.projectId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    fun observeAllWithCount(): Flow<List<ProjectWithCount>>

    @Query("SELECT * FROM project WHERE id = :id LIMIT 1")
    suspend fun getOne(id: Long): ProjectEntity?

    @Query("UPDATE project SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("DELETE FROM project WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProjectEntity): Long
}

