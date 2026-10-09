package com.innogen.aipro.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.innogen.aipro.data.local.dao.ProjectDao
import com.innogen.aipro.data.local.entities.ProjectEntity
import com.innogen.aipro.data.local.entities.toDomain
import com.innogen.aipro.data.local.entities.toEntity
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
    private val projectDao: ProjectDao,
    private val firestore : FirebaseFirestore
) : ProjectRepository {

    private val gson = Gson()
    private var activeUidListener: ListenerRegistration? = null
    private var activeEmailListener: ListenerRegistration? = null
    private var activeUserId: String? = null
    private var activeUserEmail: String? = null

    override fun getProjects(userId: String): Flow<List<Project>> {
        val effectiveUserId = if (userId.isNotBlank()) userId else (FirebaseAuth.getInstance().currentUser?.uid ?: "")
        if (effectiveUserId.isNotBlank()) {
            startRealtimeSync(effectiveUserId)
        }
        return projectDao.getProjectsByUser(effectiveUserId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun startRealtimeSync(userId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userEmail = currentUser?.email ?: ""

        if (activeUserId == userId && activeUserEmail == userEmail && (activeUidListener != null || activeEmailListener != null)) {
            return
        }

        activeUidListener?.remove()
        activeEmailListener?.remove()
        activeUserId = userId
        activeUserEmail = userEmail

        if (userId.isBlank() && userEmail.isBlank()) return

        // 1. Listen by userId (UID)
        if (userId.isNotBlank()) {
            activeUidListener = firestore.collection("projects")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("ProjectRepository", "Firestore UID listener error: ${error?.message}")
                        return@addSnapshotListener
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        processFirestoreSnapshot(snapshot, userId, checkRemovals = true)
                    }
                }
        }

        // 2. Listen by userEmail if available (allows instant cross-sync if created with email identifier)
        if (userEmail.isNotBlank()) {
            activeEmailListener = firestore.collection("projects")
                .whereEqualTo("userEmail", userEmail)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("ProjectRepository", "Firestore Email listener error: ${error?.message}")
                        return@addSnapshotListener
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        processFirestoreSnapshot(snapshot, userId, checkRemovals = false)
                    }
                }
        }
    }

    private suspend fun processFirestoreSnapshot(snapshot: QuerySnapshot, targetUserId: String, checkRemovals: Boolean) {
        val firestoreIds = mutableSetOf<String>()
        for (doc in snapshot.documents) {
            firestoreIds.add(doc.id)
            val entity = parseDocumentToEntity(doc, targetUserId)
            projectDao.upsertProject(entity)
        }

        // Handle remote deletions: if a local project for this user was deleted remotely in Firestore,
        // clean it up locally after 10s grace period (so new local projects have time to upload)
        if (checkRemovals && !snapshot.metadata.hasPendingWrites()) {
            val localIds = projectDao.getAllIdsForUser(targetUserId)
            val now = System.currentTimeMillis()
            for (localId in localIds) {
                if (!firestoreIds.contains(localId)) {
                    val localProject = projectDao.getProjectById(localId)
                    if (localProject != null && (now - localProject.createdAt) > 10000L) {
                        projectDao.deleteProject(localId)
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

        val generatedCodeJson = doc.get("generatedCode")?.let {
            if (it is String) it else gson.toJson(it)
        }

        val techStackJson = doc.get("techStack")?.let {
            if (it is String) it else gson.toJson(it)
        }

        val rawFeatures = doc.get("features") as? List<*> ?: doc.get("tags") as? List<*> ?: emptyList<Any>()
        val featuresList = rawFeatures.mapNotNull { it?.toString() }

        // Always associate with the target user's local Room ID so Room query returns it
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

    override suspend fun getProjectById(id: String): Project? =
        projectDao.getProjectById(id)?.toDomain()

    override suspend fun saveProject(project: Project) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val effectiveUserId = if (project.userId.isNotBlank()) project.userId else (currentUser?.uid ?: "")
        val userEmail = currentUser?.email ?: ""
        val updatedProject = project.copy(userId = effectiveUserId)

        // Step 1: Always save locally first (instant, works offline)
        projectDao.upsertProject(updatedProject.toEntity())

        // Step 2: Firestore sync
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
            Log.d("ProjectRepository", "Successfully synced project to Firestore: ${updatedProject.id}")
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Firestore save error: ${e.message}", e)
        }
    }

    override suspend fun deleteProject(id: String) {
        projectDao.deleteProject(id)
        try {
            firestore.collection("projects").document(id).delete().await()
        } catch (e: Exception) {
            Log.w("ProjectRepository", "Firestore delete error: ${e.message}")
        }
    }

    override suspend fun syncWithFirestore(userId: String) {
        val effectiveUserId = if (userId.isNotBlank()) userId else (FirebaseAuth.getInstance().currentUser?.uid ?: "")
        if (effectiveUserId.isBlank()) return

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: ""

        try {
            val snapshot = firestore.collection("projects")
                .whereEqualTo("userId", effectiveUserId)
                .get()
                .await()
            processFirestoreSnapshot(snapshot, effectiveUserId, checkRemovals = false)

            if (email.isNotBlank()) {
                val emailSnapshot = firestore.collection("projects")
                    .whereEqualTo("userEmail", email)
                    .get()
                    .await()
                processFirestoreSnapshot(emailSnapshot, effectiveUserId, checkRemovals = false)
            }
        } catch (e: Exception) {
            Log.w("ProjectRepository", "One-shot Firestore sync failed: ${e.message}")
        }
    }
}