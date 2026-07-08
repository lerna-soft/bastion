package com.bastion.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY created DESC")
    fun getAllKeys(): Flow<List<ApiKey>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: ApiKey): Long

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteById(id: Long)
}
