package com.innogen.aipro.data.remote

import com.google.gson.Gson
import com.innogen.aipro.data.remote.api.*
import com.innogen.aipro.domain.model.*
import com.innogen.aipro.domain.repository.AIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Groq-backed AI repository - 100% FREE, no card needed.
 * Uses Llama 3.3 70B via Groq API (OpenAI-compatible format).
 * Free tier: 30 requests/minute, 14,400 requests/day
 */
class AIRepositoryImpl @Inject constructor(
    private val openAIService: OpenAIService,
    @Named("openai_key") private val apiKey: String
) : AIRepository {

    private val gson = Gson()

    private val systemPrompt = """
You are InnoGen AI, an expert software architect and full-stack developer.
Given an app idea, respond ONLY with a valid JSON object. No markdown, no code fences, no explanation.

Required JSON structure:
{
  "title": "App Name",
  "description": "One sentence description",
  "features": ["Feature 1", "Feature 2", "Feature 3", "Feature 4", "Feature 5"],
  "techStack": {
    "frontend": "React Native",
    "backend": "Node.js + Express",
    "database": "PostgreSQL",
    "deployment": "Docker + AWS"
  },
  "frontendCode": "// Complete frontend code",
  "backendCode": "// Complete backend API code",
  "databaseSchema": "-- Complete SQL schema",
  "dockerConfig": "FROM node:18\nWORKDIR /app\nCOPY . .\nRUN npm install\nEXPOSE 3000\nCMD [\"node\",\"server.js\"]",
  "readme": "# App Name\n## Setup\n...",
  "apiDocs": "## Endpoints\nGET /api/users\nPOST /api/auth/login",
  "testCases": "// Jest tests here"
}
Return ONLY the JSON object. Nothing else.
""".trimIndent()

    override suspend fun generateApp(prompt: String): Result<Project> =
        withContext(Dispatchers.IO) {
            val models = listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b")
            var lastError: Exception? = null

            for (model in models) {
                try {
                    val request = GeminiRequest(
                        model = model,
                        messages = listOf(
                            GeminiContent(role = "system", content = systemPrompt),
                            GeminiContent(role = "user",   content = "Generate a complete app for: $prompt")
                        ),
                        maxTokens = 8192,
                        maxCompletionTokens = 8192
                    )
                    val response = openAIService.generateCompletion(
                        apiKey  = "Bearer $apiKey",
                        request = request
                    )
                    if (response.isSuccessful) {
                        val content = response.body()
                            ?.choices?.firstOrNull()
                            ?.message?.content
                        if (!content.isNullOrBlank()) {
                            return@withContext Result.success(parseGenerationResponse(content, prompt))
                        }
                    } else {
                        lastError = Exception("Groq API error ($model): ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
            Result.failure(lastError ?: Exception("AI generation failed across all models"))
        }

    override suspend fun regenerateSection(projectId: String, instruction: String): Result<Project> =
        withContext(Dispatchers.IO) {
            val models = listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b", "qwen/qwen3.8-27b")
            var lastError: Exception? = null

            for (model in models) {
                try {
                    val request = GeminiRequest(
                        model = model,
                        messages = listOf(
                            GeminiContent(role = "system", content = systemPrompt),
                            GeminiContent(role = "user",   content = "Update the app with: $instruction. Return full updated JSON.")
                        ),
                        maxTokens = 8192,
                        maxCompletionTokens = 8192
                    )
                    val response = openAIService.generateCompletion(
                        apiKey  = "Bearer $apiKey",
                        request = request
                    )
                    if (response.isSuccessful) {
                        val content = response.body()
                            ?.choices?.firstOrNull()
                            ?.message?.content
                        if (!content.isNullOrBlank()) {
                            return@withContext Result.success(parseGenerationResponse(content, instruction).copy(id = projectId))
                        }
                    } else {
                        lastError = Exception("Groq API error ($model): ${response.code()} - ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
            Result.failure(lastError ?: Exception("Empty response"))
        }

    override suspend fun detectBugs(code: String): Result<List<BugReport>> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
Analyze this code for bugs. Return ONLY a JSON array, no other text:
[{"line":10,"description":"Null pointer dereference","severity":"HIGH","fix":"Add null check"}]
Severity values: LOW, MEDIUM, HIGH, CRITICAL
Code:
${code.take(3000)}
""".trimIndent()
                val request = GeminiRequest(
                    messages  = listOf(GeminiContent(role = "user", content = prompt)),
                    maxTokens = 2000
                )
                val response = openAIService.generateCompletion("Bearer $apiKey", request)
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
                val bugs = try {
                    gson.fromJson(extractJson(text), Array<BugReport>::class.java).toList()
                } catch (e: Exception) { emptyList() }
                Result.success(bugs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun analyzeSecurity(code: String): Result<List<SecurityIssue>> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
Analyze for security vulnerabilities. Return ONLY a JSON array:
[{"title":"SQL Injection","description":"Input not sanitized","risk":"CRITICAL","mitigation":"Use parameterized queries"}]
Risk values: LOW, MEDIUM, HIGH, CRITICAL
Code:
${code.take(3000)}
""".trimIndent()
                val request = GeminiRequest(
                    messages  = listOf(GeminiContent(role = "user", content = prompt)),
                    maxTokens = 2000
                )
                val response = openAIService.generateCompletion("Bearer $apiKey", request)
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
                val issues = try {
                    gson.fromJson(extractJson(text), Array<SecurityIssue>::class.java).toList()
                } catch (e: Exception) { emptyList() }
                Result.success(issues)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun generateTests(code: String): Result<List<TestCase>> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
Generate test cases. Return ONLY a JSON array:
[{"name":"should login successfully","description":"Tests login","code":"test('login', async () => { expect(res.status).toBe(200) })","type":"UNIT"}]
Type values: UNIT, INTEGRATION, E2E
Code:
${code.take(3000)}
""".trimIndent()
                val request = GeminiRequest(
                    messages  = listOf(GeminiContent(role = "user", content = prompt)),
                    maxTokens = 2000
                )
                val response = openAIService.generateCompletion("Bearer $apiKey", request)
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
                val tests = try {
                    gson.fromJson(extractJson(text), Array<TestCase>::class.java).toList()
                } catch (e: Exception) { emptyList() }
                Result.success(tests)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseGenerationResponse(content: String, prompt: String): Project {
        val cleanJson = extractJson(content)
        return try {
            val raw = gson.fromJson(cleanJson, Map::class.java)
            val features = (raw["features"] as? List<*>)
                ?.filterIsInstance<String>()
                ?: listOf("Authentication", "Dashboard", "Data Management", "Notifications", "Settings")
            val tsMap = raw["techStack"] as? Map<*, *>
            val techStack = TechStack(
                frontend   = tsMap?.get("frontend")?.toString()   ?: "React Native",
                backend    = tsMap?.get("backend")?.toString()    ?: "Node.js + Express",
                database   = tsMap?.get("database")?.toString()   ?: "PostgreSQL",
                deployment = tsMap?.get("deployment")?.toString() ?: "Docker + AWS"
            )
            val generatedCode = GeneratedCode(
                frontendCode   = raw["frontendCode"]?.toString()   ?: "",
                backendCode    = raw["backendCode"]?.toString()    ?: "",
                databaseSchema = raw["databaseSchema"]?.toString() ?: "",
                dockerConfig   = raw["dockerConfig"]?.toString()   ?: "",
                readme         = raw["readme"]?.toString()         ?: "",
                apiDocs        = raw["apiDocs"]?.toString()        ?: "",
                testCases      = raw["testCases"]?.toString()      ?: ""
            )
            Project(
                id            = UUID.randomUUID().toString(),
                title         = raw["title"]?.toString()       ?: "My App",
                description   = raw["description"]?.toString() ?: prompt,
                prompt        = prompt,
                status        = ProjectStatus.COMPLETE,
                createdAt     = System.currentTimeMillis(),
                updatedAt     = System.currentTimeMillis(),
                generatedCode = generatedCode,
                techStack     = techStack,
                features      = features
            )
        } catch (e: Exception) {
            Project(
                id            = UUID.randomUUID().toString(),
                title         = "Generated App",
                description   = prompt,
                prompt        = prompt,
                status        = ProjectStatus.COMPLETE,
                generatedCode = GeneratedCode(
                    frontendCode = content,
                    readme       = "# Generated App\n\nPrompt: $prompt\n\n$content"
                )
            )
        }
    }

    private fun extractJson(text: String): String {
        val stripped = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val start = stripped.indexOfFirst { it == '{' || it == '[' }
        val end   = stripped.indexOfLast  { it == '}' || it == ']' }
        return if (start >= 0 && end > start) stripped.substring(start, end + 1) else stripped
    }
}