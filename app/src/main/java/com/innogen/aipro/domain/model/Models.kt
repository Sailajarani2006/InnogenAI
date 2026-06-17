package com.innogen.aipro.domain.model

import com.google.gson.annotations.SerializedName

// ── Core domain models ────────────────────────────────────────────────────────

data class User(
    val uid      : String = "",
    val email    : String = "",
    val name     : String = "",
    val photoUrl : String = "",
    val plan     : String = "free"
)

data class Project(
    val id           : String = "",
    val userId       : String = "",
    val title        : String = "",
    val description  : String = "",
    val prompt       : String = "",
    val status       : ProjectStatus = ProjectStatus.DRAFT,
    val createdAt    : Long   = System.currentTimeMillis(),
    val updatedAt    : Long   = System.currentTimeMillis(),
    val generatedCode: GeneratedCode? = null,
    val techStack    : TechStack? = null,
    val githubRepo   : String = "",
    val features     : List<String> = emptyList()
)

enum class ProjectStatus { DRAFT, GENERATING, COMPLETE, ERROR }

data class GeneratedCode(
    val frontendCode : String = "",
    val backendCode  : String = "",
    val databaseSchema: String = "",
    val dockerConfig : String = "",
    val readme       : String = "",
    val apiDocs      : String = "",
    val testCases    : String = ""
)

data class TechStack(
    val frontend : String = "",
    val backend  : String = "",
    val database : String = "",
    val deployment: String = ""
)

data class GenerationStep(
    val id      : Int,
    val title   : String,
    val description: String,
    val status  : StepStatus = StepStatus.PENDING
)

enum class StepStatus { PENDING, IN_PROGRESS, COMPLETE, ERROR }

data class CodeFile(
    val name    : String,
    val path    : String,
    val content : String,
    val language: String
)

data class BugReport(
    val line       : Int,
    val description: String,
    val severity   : BugSeverity,
    val fix        : String
)

enum class BugSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class SecurityIssue(
    val title      : String,
    val description: String,
    val risk       : RiskLevel,
    val mitigation : String
)

enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class TestCase(
    val name       : String,
    val description: String,
    val code       : String,
    val type       : TestType
)

enum class TestType { UNIT, INTEGRATION, E2E }
