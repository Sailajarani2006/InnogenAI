package com.innogen.aipro.data.remote

import com.google.gson.Gson
import com.innogen.aipro.BuildConfig
import com.innogen.aipro.data.remote.api.*
import com.innogen.aipro.domain.model.*
import com.innogen.aipro.domain.repository.AIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

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
            try {
                // FIX-07 (HIGH-003): Sanitize user input before embedding in LLM prompt
                val safePrompt = sanitizeUserInput(prompt)
                val request = OpenAIRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user",   content = "Generate a complete app for: $safePrompt")
                    )
                )
                val response = openAIService.generateCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                if (!response.isSuccessful) {
                    // FIX (LOW-002): Don't leak raw API error body to caller in release
                    val detail = if (BuildConfig.DEBUG) " - ${response.errorBody()?.string()}" else ""
                    return@withContext Result.failure(
                        Exception("AI generation failed (${response.code()})$detail")
                    )
                }
                val content = response.body()
                    ?.choices?.firstOrNull()
                    ?.message?.content
                    ?: return@withContext Result.failure(Exception("Empty response from AI service"))
                Result.success(parseGenerationResponse(content, prompt))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun regenerateSection(projectId: String, instruction: String): Result<Project> =
        withContext(Dispatchers.IO) {
            try {
                // FIX-07 (HIGH-003): Sanitize instruction before embedding in LLM prompt
                val safeInstruction = sanitizeUserInput(instruction)
                val request = OpenAIRequest(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user",   content = "Update the app with: $safeInstruction. Return full updated JSON.")
                    )
                )
                val response = openAIService.generateCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
                if (!response.isSuccessful) {
                    val detail = if (BuildConfig.DEBUG) " - ${response.errorBody()?.string()}" else ""
                    return@withContext Result.failure(
                        Exception("AI regeneration failed (${response.code()})$detail")
                    )
                }
                val content = response.body()
                    ?.choices?.firstOrNull()
                    ?.message?.content
                    ?: return@withContext Result.failure(Exception("Empty response from AI service"))
                Result.success(parseGenerationResponse(content, instruction).copy(id = projectId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun detectBugs(code: String): Result<List<BugReport>> =
        withContext(Dispatchers.IO) {
            try {
                // FIX-07 (HIGH-004): Sanitize code content before embedding in LLM prompt
                val safeCode = sanitizeCodeInput(code)
                val prompt = """
Analyze this code for bugs. Return ONLY a JSON array, no other text:
[{"line":10,"description":"Null pointer dereference","severity":"HIGH","fix":"Add null check"}]
Severity values: LOW, MEDIUM, HIGH, CRITICAL
Code:
$safeCode
""".trimIndent()
                val request = OpenAIRequest(
                    model = "llama-3.3-70b-versatile",
                    messages  = listOf(ChatMessage(role = "user", content = prompt)),
                    maxTokens = 2000
                )
                val response = openAIService.generateCompletion("Bearer $apiKey", request)
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
                val bugs = try {
                    gson.fromJson(extractJson(text), Array<BugReport>::class.java).toList()
                } catch (ignored: Exception) { emptyList() }
                Result.success(bugs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun analyzeSecurity(code: String): Result<List<SecurityIssue>> =
        withContext(Dispatchers.IO) {
            try {
                // FIX-07 (HIGH-004): Sanitize code content before embedding in LLM prompt
                val safeCode = sanitizeCodeInput(code)
                val prompt = """
Analyze for security vulnerabilities. Return ONLY a JSON array:
[{"title":"SQL Injection","description":"Input not sanitized","risk":"CRITICAL","mitigation":"Use parameterized queries"}]
Risk values: LOW, MEDIUM, HIGH, CRITICAL
Code:
$safeCode
""".trimIndent()
                val request = OpenAIRequest(
                    model = "llama-3.3-70b-versatile",
                    messages  = listOf(ChatMessage(role = "user", content = prompt)),
                    maxTokens = 2000
                )
                val response = openAIService.generateCompletion("Bearer $apiKey", request)
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
                val issues = try {
                    gson.fromJson(extractJson(text), Array<SecurityIssue>::class.java).toList()
                } catch (ignored: Exception) { emptyList() }
                Result.success(issues)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun generateTests(code: String): Result<List<TestCase>> =
        withContext(Dispatchers.IO) {
            try {
                // FIX-07 (HIGH-004): Sanitize code content before embedding in LLM prompt
                val safeCode = sanitizeCodeInput(code)
                val prompt = """
Generate test cases. Return ONLY a JSON array:
[{"name":"should login successfully","description":"Tests login","code":"test('login', async () => { expect(res.status).toBe(200) })","type":"UNIT"}]
Type values: UNIT, INTEGRATION, E2E
Code:
$safeCode
""".trimIndent()
                val request = OpenAIRequest(
                    model = "llama-3.3-70b-versatile",
                    messages  = listOf(ChatMessage(role = "user", content = prompt)),
                    maxTokens = 2000
                )
                val response = openAIService.generateCompletion("Bearer $apiKey", request)
                val text = response.body()?.choices?.firstOrNull()?.message?.content ?: "[]"
                val tests = try {
                    gson.fromJson(extractJson(text), Array<TestCase>::class.java).toList()
                } catch (ignored: Exception) { emptyList() }
                Result.success(tests)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun parseGenerationResponse(content: String, prompt: String): Project {
        val cleanJson = extractJson(content)
        return try {
            val raw      = gson.fromJson(cleanJson, Map::class.java)
            val features = (raw["features"] as? List<*>)
                ?.filterIsInstance<String>()
                ?: listOf("Authentication", "Dashboard", "Data Management", "Notifications", "Settings")
            val tsMap     = raw["techStack"] as? Map<*, *>
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
        return if (start in 0 until end) stripped.substring(start, end + 1) else stripped
    }

    // ── FIX-07 (HIGH-003): Prompt injection sanitization ──────────────────────

    /**
     * Sanitizes free-text user input (prompts, instructions) before embedding
     * into LLM messages. Removes known adversarial injection patterns and
     * enforces a maximum character limit.
     */
    private fun sanitizeUserInput(input: String): String {
        val injectionPatterns = listOf(
            Regex("ignore\\s+(all\\s+)?previous\\s+instructions?", RegexOption.IGNORE_CASE),
            Regex("forget\\s+(all\\s+)?instructions?", RegexOption.IGNORE_CASE),
            Regex("system\\s*prompt", RegexOption.IGNORE_CASE),
            Regex("reveal\\s+(your\\s+)?(api\\s+)?key", RegexOption.IGNORE_CASE),
            Regex("output\\s+(all\\s+)?credentials?", RegexOption.IGNORE_CASE),
            Regex("pretend\\s+(you\\s+are|to\\s+be)", RegexOption.IGNORE_CASE),
            Regex("act\\s+as\\s+if", RegexOption.IGNORE_CASE),
            Regex("jailbreak", RegexOption.IGNORE_CASE),
            Regex("dan\\s*mode", RegexOption.IGNORE_CASE),
            Regex("disregard\\s+(all\\s+)?previous", RegexOption.IGNORE_CASE)
        )
        var sanitized = input.trim()
        for (pattern in injectionPatterns) {
            sanitized = sanitized.replace(pattern, "[filtered]")
        }
        return sanitized.take(2000) // hard character limit
    }

    /**
     * Sanitizes code submitted for analysis before embedding in LLM prompts.
     * Strips single-line and block comments that could contain injection
     * payloads, then truncates to a safe length.
     */
    private fun sanitizeCodeInput(code: String): String {
        // Strip single-line comments that may contain injection instructions
        val noLineComments = code.replace(
            Regex("//[^\n]*ignore[^\n]*previous[^\n]*instructions?[^\n]*", RegexOption.IGNORE_CASE),
            "// [comment filtered]"
        )
        // Strip block comments with injection patterns
        val noBlockComments = noLineComments.replace(
            Regex("/\\*[^*]*ignore[^*]*previous[^*]*instructions?[^*]*\\*/", RegexOption.IGNORE_CASE),
            "/* [comment filtered] */"
        )
        return noBlockComments.take(3000) // hard character limit
    }
}
