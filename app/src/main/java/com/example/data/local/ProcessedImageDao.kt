package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedImageDao {
    @Query("SELECT * FROM processed_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<ProcessedImageEntity>>

    @Query("SELECT * FROM processed_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: Long): ProcessedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ProcessedImageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ProcessedImageEntity>): List<Long>

    @Query("DELETE FROM processed_images WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM processed_images WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM processed_images")
    suspend fun clearAll()
}
