package com.innogen.aipro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.innogen.aipro.data.local.dao.ProjectDao
import com.innogen.aipro.data.local.entities.toDomain
import com.innogen.aipro.data.local.entities.toEntity
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val firestore : FirebaseFirestore
) : ProjectRepository {

    private val gson = Gson()

    override fun getProjects(userId: String): Flow<List<Project>> =
        projectDao.getProjectsByUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getProjectById(id: String): Project? =
        projectDao.getProjectById(id)?.toDomain()

    override suspend fun saveProject(project: Project) {
        // Save locally first (offline-first)
        projectDao.upsertProject(project.toEntity())

        // Then sync to Firestore (best-effort)
        try {
            val data = mapOf(
                "id"           to project.id,
                "userId"       to project.userId,
                "title"        to project.title,
                "description"  to project.description,
                "prompt"       to project.prompt,
                "status"       to project.status.name,
                "createdAt"    to project.createdAt,
                "updatedAt"    to project.updatedAt,
                "features"     to project.features,
                "githubRepo"   to project.githubRepo,
                "techStack"    to project.techStack?.let {
                    mapOf("frontend" to it.frontend, "backend" to it.backend,
                          "database" to it.database, "deployment" to it.deployment)
                },
                // Store code separately to avoid Firestore 1 MB limit
                "hasCode"      to (project.generatedCode != null)
            )
            kotlinx.coroutines.withTimeoutOrNull(3000) {
                firestore.collection("projects")
                    .document(project.id)
                    .set(data, SetOptions.merge())
                    .await()
            }
        } catch (_: Exception) {
            // Firestore sync failure is non-fatal; local DB has the data
        }
    }

    override suspend fun deleteProject(id: String) {
        projectDao.deleteProject(id)
        try {
            kotlinx.coroutines.withTimeoutOrNull(3000) {
                firestore.collection("projects").document(id).delete().await()
            }
        } catch (_: Exception) {}
    }

}
