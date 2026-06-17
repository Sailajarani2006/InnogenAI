package com.innogen.aipro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.innogen.aipro.domain.model.*

// ── Room Entity ───────────────────────────────────────────────────────────────

@Entity(tableName = "projects")
@TypeConverters(ProjectConverters::class)
data class ProjectEntity(
    @PrimaryKey val id           : String,
    val userId      : String,
    val title       : String,
    val description : String,
    val prompt      : String,
    val status      : String,           // ProjectStatus.name
    val createdAt   : Long,
    val updatedAt   : Long,
    val generatedCode: String?,         // JSON serialised GeneratedCode
    val techStack   : String?,          // JSON serialised TechStack
    val githubRepo  : String,
    val features    : String            // JSON list of strings
)

// ── Converters ────────────────────────────────────────────────────────────────

class ProjectConverters {
    private val gson = Gson()

    @TypeConverter fun fromStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter fun toStringList(value: List<String>): String =
        gson.toJson(value)
}

// ── Mapper extensions ─────────────────────────────────────────────────────────

private val gson = Gson()

fun ProjectEntity.toDomain() = Project(
    id            = id,
    userId        = userId,
    title         = title,
    description   = description,
    prompt        = prompt,
    status        = ProjectStatus.valueOf(status),
    createdAt     = createdAt,
    updatedAt     = updatedAt,
    generatedCode = generatedCode?.let { gson.fromJson(it, GeneratedCode::class.java) },
    techStack     = techStack?.let { gson.fromJson(it, TechStack::class.java) },
    githubRepo    = githubRepo,
    features      = gson.fromJson(features, object : TypeToken<List<String>>() {}.type) ?: emptyList()
)

fun Project.toEntity() = ProjectEntity(
    id            = id,
    userId        = userId,
    title         = title,
    description   = description,
    prompt        = prompt,
    status        = status.name,
    createdAt     = createdAt,
    updatedAt     = updatedAt,
    generatedCode = generatedCode?.let { gson.toJson(it) },
    techStack     = techStack?.let { gson.toJson(it) },
    githubRepo    = githubRepo,
    features      = gson.toJson(features)
)
