package com.timetrackermae.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: Project): Long

    @Query("SELECT * FROM projects ORDER BY id ASC")
    fun getAll(): Flow<List<Project>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int
}
