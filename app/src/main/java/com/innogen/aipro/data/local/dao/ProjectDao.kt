package com.innogen.aipro.data.local.dao

import androidx.room.*
import com.innogen.aipro.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    /** Observe all projects for current user (reactive Flow) */
    @Query("SELECT * FROM projects WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getProjectsByUser(userId: String): Flow<List<ProjectEntity>>

    /** One-shot fetch by id */
    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    /** Insert or replace (upsert) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    /** Delete by id */
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    /** Delete all projects for user */
    @Query("DELETE FROM projects WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    /** Count projects */
    @Query("SELECT COUNT(*) FROM projects WHERE userId = :userId")
    suspend fun countProjects(userId: String): Int
}
