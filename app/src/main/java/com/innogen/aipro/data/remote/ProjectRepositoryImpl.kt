package com.innogen.aipro.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.innogen.aipro.data.local.dao.ProjectDao
import com.innogen.aipro.data.local.entities.ProjectEntity
import com.innogen.aipro.data.local.entities.toDomain
import com.innogen.aipro.data.local.entities.toEntity
import com.innogen.aipro.data.remote.supabase.SupabaseApiService
import com.innogen.aipro.data.remote.supabase.SupabaseProjectDto
import com.innogen.aipro.data.remote.supabase.SupabaseRealtimeManager
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProjectRepositoryImpl @Inject constructor(
    private val projectDao       : ProjectDao,
    private val firestore        : FirebaseFirestore,
    private val supabaseApi      : SupabaseApiService,
    private val supabaseRealtime : SupabaseRealtimeManager
) : ProjectRepository {

    private val gson = Gson()

    private var activeSyncUserId: String? = null
    private var activeSyncListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Setup Supabase Real-Time WebSocket listeners
        supabaseRealtime.onProjectUpserted = { dto ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entity = dto.toProjectEntity()
                    projectDao.upsertProject(entity)
                    Log.d("ProjectRepository", "Realtime Supabase project upserted: ${dto.id}")
                } catch (e: Exception) {
                    Log.w("ProjectRepository", "Failed to handle Supabase upsert: ${e.message}")
                }
            }
        }

        supabaseRealtime.onProjectDeleted = { deletedId ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    projectDao.deleteProject(deletedId)
                    Log.d("ProjectRepository", "Realtime Supabase project deleted: $deletedId")
                } catch (e: Exception) {
                    Log.w("ProjectRepository", "Failed to handle Supabase delete: ${e.message}")
                }
            }
        }

        // Connect WebSocket
        supabaseRealtime.connect()
    }

    override fun getProjects(userId: String): Flow<List<Project>> {
        val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val effectiveUserId = if (userId.isNotBlank()) userId else currentAuthUid

        if (effectiveUserId.isNotBlank()) {
            startRealtimeRemoteSync(effectiveUserId)
        }

        return projectDao.getProjectsByUser(effectiveUserId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun startRealtimeRemoteSync(userId: String) {
        if (activeSyncUserId == userId && activeSyncListener != null) return

        activeSyncListener?.remove()
        activeSyncUserId = userId

        // 1. Initial sync with Supabase PostgREST
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = supabaseApi.getProjects(userFilter = "eq.$userId")
                if (res.isSuccessful && res.body() != null) {
                    val supabaseProjects = res.body()!!
                    for (sp in supabaseProjects) {
                        projectDao.upsertProject(sp.toProjectEntity())
                    }
                    Log.d("ProjectRepository", "Synced ${supabaseProjects.size} projects from Supabase")
                }
            } catch (e: Exception) {
                Log.w("ProjectRepository", "Supabase initial fetch notice: ${e.message}")
            }
        }

        // 2. Dual sync with Firestore
        activeSyncListener = firestore.collection("projects")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                CoroutineScope(Dispatchers.IO).launch {
                    val firestoreDocIds = mutableSetOf<String>()
                    for (doc in snapshot.documents) {
                        try {
                            firestoreDocIds.add(doc.id)
                            val entity = parseDocumentToEntity(doc, userId)
                            projectDao.upsertProject(entity)
                        } catch (e: Exception) {
                            Log.w("ProjectRepository", "Error parsing remote project: ${e.message}")
                        }
                    }

                    // Remove local projects that were deleted remotely
                    if (!snapshot.metadata.hasPendingWrites()) {
                        val localIds = projectDao.getAllIdsForUser(userId)
                        for (localId in localIds) {
                            if (!firestoreDocIds.contains(localId)) {
                                projectDao.deleteProject(localId)
                            }
                        }
                    }
                }
            }
    }

    private fun parseDocumentToEntity(doc: DocumentSnapshot, fallbackUserId: String): ProjectEntity {
        val title = doc.getString("title") ?: doc.getString("name") ?: doc.getString("idea") ?: "Untitled"
        val desc = doc.getString("description") ?: ""
        val prompt = doc.getString("prompt") ?: doc.getString("idea") ?: title
        val status = doc.getString("status") ?: "COMPLETE"

        val createdAt = when (val c = doc.get("createdAt")) {
            is Number -> c.toLong()
            is com.google.firebase.Timestamp -> c.toDate().time
            else -> System.currentTimeMillis()
        }

        val updatedAt = when (val u = doc.get("updatedAt")) {
            is Number -> u.toLong()
            is com.google.firebase.Timestamp -> u.toDate().time
            else -> createdAt
        }

        val generatedCodeObj = doc.get("generatedCode")
        val generatedCodeJson = when (generatedCodeObj) {
            is String -> generatedCodeObj
            is Map<*, *> -> gson.toJson(generatedCodeObj)
            else -> {
                val frontend = doc.getString("frontendCode") ?: ""
                val backend = doc.getString("backendCode") ?: ""
                val schema = doc.getString("databaseSchema") ?: ""
                val readme = doc.getString("readme") ?: ""
                if (frontend.isNotBlank() || backend.isNotBlank() || schema.isNotBlank() || readme.isNotBlank()) {
                    gson.toJson(mapOf(
                        "frontendCode"   to frontend,
                        "backendCode"    to backend,
                        "databaseSchema" to schema,
                        "readme"         to readme
                    ))
                } else null
            }
        }

        val techStackObj = doc.get("techStack")
        val techStackJson = when (techStackObj) {
            is String -> techStackObj
            is Map<*, *> -> gson.toJson(techStackObj)
            else -> null
        }

        val rawFeatures = doc.get("features") as? List<*> ?: doc.get("tags") as? List<*> ?: emptyList<Any>()
        val featuresList = rawFeatures.mapNotNull { it?.toString() }

        val docUserId = doc.getString("userId")
        val effectiveUserId = if (!docUserId.isNullOrBlank()) docUserId else fallbackUserId

        return ProjectEntity(
            id            = doc.id,
            userId        = effectiveUserId,
            title         = title,
            description   = desc,
            prompt        = prompt,
            status        = status,
            createdAt     = createdAt,
            updatedAt     = updatedAt,
            generatedCode = generatedCodeJson,
            techStack     = techStackJson,
            githubRepo    = doc.getString("githubRepo") ?: "",
            features      = gson.toJson(featuresList)
        )
    }

    override suspend fun getProjectById(id: String): Project? {
        val local = projectDao.getProjectById(id)
        if (local != null) return local.toDomain()

        // Fallback fetch from Firestore if not in local Room DB yet
        return try {
            val doc = firestore.collection("projects").document(id).get().await()
            if (doc.exists()) {
                val currentAuthUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val entity = parseDocumentToEntity(doc, currentAuthUid)
                projectDao.upsertProject(entity)
                entity.toDomain()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveProject(project: Project) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val effectiveUserId = if (project.userId.isNotBlank()) project.userId else (currentUser?.uid ?: "")
        val userEmail = currentUser?.email ?: ""
        val updatedProject = project.copy(userId = effectiveUserId)

        // 1. Save locally to Room DB immediately (instant, offline-ready)
        projectDao.upsertProject(updatedProject.toEntity())

        // 2. Sync to Supabase in background
        if (effectiveUserId.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dto = updatedProject.toSupabaseDto(userEmail)
                    val res = supabaseApi.upsertProject(project = dto)
                    if (res.isSuccessful) {
                        Log.d("ProjectRepository", "Successfully synced project to Supabase: ${updatedProject.id}")
                    } else {
                        Log.w("ProjectRepository", "Supabase upsert code: ${res.code()}")
                    }
                } catch (e: Exception) {
                    Log.w("ProjectRepository", "Supabase sync notice: ${e.message}")
                }
            }

            // 3. Sync to Firestore in background
            try {
                val data = mutableMapOf<String, Any>(
                    "id"          to updatedProject.id,
                    "userId"      to effectiveUserId,
                    "userEmail"   to userEmail,
                    "title"       to updatedProject.title,
                    "name"        to updatedProject.title,
                    "description" to updatedProject.description,
                    "prompt"      to updatedProject.prompt,
                    "idea"        to updatedProject.prompt,
                    "status"      to updatedProject.status.name,
                    "createdAt"   to updatedProject.createdAt,
                    "updatedAt"   to updatedProject.updatedAt,
                    "features"    to updatedProject.features,
                    "tags"        to (if (updatedProject.features.isNotEmpty()) updatedProject.features else listOfNotNull(updatedProject.techStack?.frontend, updatedProject.techStack?.backend, updatedProject.techStack?.database)),
                    "githubRepo"  to updatedProject.githubRepo,
                    "hasCode"     to (updatedProject.generatedCode != null)
                )

                updatedProject.generatedCode?.let {
                    data["generatedCode"] = mapOf(
                        "frontendCode"    to it.frontendCode,
                        "backendCode"     to it.backendCode,
                        "databaseSchema"  to it.databaseSchema,
                        "dockerConfig"    to it.dockerConfig,
                        "readme"          to it.readme,
                        "apiDocs"         to it.apiDocs,
                        "testCases"       to it.testCases
                    )
                }

                updatedProject.techStack?.let {
                    data["techStack"] = mapOf(
                        "frontend"   to it.frontend,
                        "backend"    to it.backend,
                        "database"   to it.database,
                        "deployment" to it.deployment
                    )
                }

                firestore.collection("projects")
                    .document(updatedProject.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d("ProjectRepository", "Synced project to Firestore: ${updatedProject.id}")
            } catch (e: Exception) {
                Log.e("ProjectRepository", "Firestore save error: ${e.message}", e)
            }
        }
    }

    override suspend fun deleteProject(id: String) {
        // 1. Delete locally from Room DB immediately
        projectDao.deleteProject(id)

        // 2. Delete from Supabase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabaseApi.deleteProject(idFilter = "eq.$id")
                Log.d("ProjectRepository", "Deleted project from Supabase: $id")
            } catch (e: Exception) {
                Log.w("ProjectRepository", "Supabase delete notice: ${e.message}")
            }
        }

        // 3. Delete from Firestore
        try {
            firestore.collection("projects").document(id).delete().await()
            Log.d("ProjectRepository", "Deleted project from Firestore: $id")
        } catch (e: Exception) {
            Log.w("ProjectRepository", "Firestore delete error: ${e.message}")
        }
    }

    override suspend fun syncWithFirestore(userId: String) {
        val effectiveUserId = if (userId.isNotBlank()) userId else (FirebaseAuth.getInstance().currentUser?.uid ?: "")
        if (effectiveUserId.isBlank()) return

        // Supabase sync
        try {
            val res = supabaseApi.getProjects(userFilter = "eq.$effectiveUserId")
            if (res.isSuccessful && res.body() != null) {
                for (sp in res.body()!!) {
                    projectDao.upsertProject(sp.toProjectEntity())
                }
            }
        } catch (e: Exception) {
            Log.w("ProjectRepository", "Supabase one-shot sync notice: ${e.message}")
        }

        // Firestore sync
        try {
            val snapshot = firestore.collection("projects")
                .whereEqualTo("userId", effectiveUserId)
                .get()
                .await()
            for (doc in snapshot.documents) {
                val entity = parseDocumentToEntity(doc, effectiveUserId)
                projectDao.upsertProject(entity)
            }
        } catch (e: Exception) {
            Log.w("ProjectRepository", "One-shot Firestore sync failed: ${e.message}")
        }
    }

    private fun SupabaseProjectDto.toProjectEntity(): ProjectEntity {
        val genCodeJson = if (generatedCode != null) gson.toJson(generatedCode) else null
        val techStackJson = if (techStack != null) gson.toJson(techStack) else null
        return ProjectEntity(
            id            = id,
            userId        = userId,
            title         = title,
            description   = description ?: "",
            prompt        = prompt ?: title,
            status        = status,
            createdAt     = createdAt,
            updatedAt     = updatedAt,
            generatedCode = genCodeJson,
            techStack     = techStackJson,
            githubRepo    = githubRepo ?: "",
            features      = gson.toJson(features)
        )
    }

    private fun Project.toSupabaseDto(userEmail: String): SupabaseProjectDto {
        val genCodeMap = generatedCode?.let {
            mapOf(
                "frontendCode"   to it.frontendCode,
                "backendCode"    to it.backendCode,
                "databaseSchema" to it.databaseSchema,
                "dockerConfig"   to it.dockerConfig,
                "readme"         to it.readme,
                "apiDocs"        to it.apiDocs,
                "testCases"      to it.testCases
            )
        }
        val techStackMap = techStack?.let {
            mapOf(
                "frontend"   to it.frontend,
                "backend"    to it.backend,
                "database"   to it.database,
                "deployment" to it.deployment
            )
        }
        return SupabaseProjectDto(
            id            = id,
            userId        = userId,
            userEmail     = userEmail,
            title         = title,
            name          = title,
            description   = description,
            prompt        = prompt,
            status        = status.name,
            createdAt     = createdAt,
            updatedAt     = updatedAt,
            features      = features,
            tags          = features,
            generatedCode = genCodeMap,
            techStack     = techStackMap,
            githubRepo    = githubRepo,
            hasCode       = (generatedCode != null)
        )
    }
}