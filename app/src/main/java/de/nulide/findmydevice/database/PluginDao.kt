package de.nulide.findmydevice.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {
    @Transaction
    @Query("SELECT * FROM plugin")
    fun getAll(): Flow<List<Plugin>>

    @Query("SELECT * FROM plugin WHERE app_id = :appId LIMIT 1")
    suspend fun get(appId: String): Plugin?

    @Insert
    suspend fun insert(plugin: Plugin)

    @Update
    suspend fun update(plugin: Plugin)

    @Delete
    suspend fun delete(plugin: Plugin)
}
