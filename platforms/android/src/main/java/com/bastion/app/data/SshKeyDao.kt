package com.bastion.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SshKeyDao {
    @Query("SELECT * FROM ssh_keys ORDER BY created DESC")
    fun getAllKeys(): Flow<List<SshKey>>

    @Query("SELECT * FROM ssh_keys WHERE id = :id")
    suspend fun getKeyById(id: Long): SshKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: SshKey): Long

    @Delete
    suspend fun delete(key: SshKey)

    @Query("DELETE FROM ssh_keys WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE ssh_keys SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}
