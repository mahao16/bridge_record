package com.record.bridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query(
        """
        SELECT label FROM dictionary
        WHERE category = :category AND isActive = 1
        ORDER BY isDefault DESC, label ASC
        """
    )
    fun observeActiveLabels(category: String): Flow<List<String>>

    @Query(
        """
        SELECT label FROM dictionary
        WHERE category = :category AND isActive = 1
        ORDER BY isDefault DESC, label ASC
        """
    )
    suspend fun listActiveLabels(category: String): List<String>

    @Query(
        """
        SELECT * FROM dictionary
        WHERE category = :category
        ORDER BY isDefault DESC, label ASC
        """
    )
    fun observeEntries(category: String): Flow<List<DictionaryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DictionaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<DictionaryEntity>): List<Long>

    @Query("UPDATE dictionary SET isActive = :isActive WHERE id = :id")
    suspend fun updateActive(id: Long, isActive: Boolean)

    @Query("UPDATE dictionary SET isActive = :isActive WHERE category = :category AND label = :label")
    suspend fun updateActiveByLabel(category: String, label: String, isActive: Boolean)

    @Query("UPDATE dictionary SET remark = :remark WHERE id = :id")
    suspend fun updateRemark(id: Long, remark: String)

    @Query("UPDATE dictionary SET remark = :remark WHERE category = :category AND label = :label")
    suspend fun updateRemarkByLabel(category: String, label: String, remark: String)

    @Query("DELETE FROM dictionary WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM dictionary WHERE category = :category AND label = :label")
    suspend fun delete(category: String, label: String)

    @Query("DELETE FROM dictionary WHERE category = :category AND isDefault = 1")
    suspend fun deleteDefaults(category: String)

    @Query("DELETE FROM dictionary WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query(
        """
        DELETE FROM dictionary
        WHERE category = :category
          AND isDefault = 1
          AND label NOT IN (:labels)
        """
    )
    suspend fun deleteDefaultLabelsNotIn(category: String, labels: List<String>)

    @Query(
        """
        DELETE FROM dictionary
        WHERE category IN (:categoryA, :categoryB)
          AND isDefault = 0
        """
    )
    suspend fun deleteNonDefaultsInTwoCategories(
        categoryA: String,
        categoryB: String
    )

    @Query(
        """
        DELETE FROM dictionary
        WHERE category IN (:categoryA, :categoryB)
        """
    )
    suspend fun deleteAllInTwoCategories(
        categoryA: String,
        categoryB: String
    )
}

