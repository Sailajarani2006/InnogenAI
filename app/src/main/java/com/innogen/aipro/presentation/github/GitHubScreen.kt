package com.innogen.aipro.presentation.github

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.presentation.*
import com.innogen.aipro.presentation.theme.*

@Composable
fun GitHubScreen(
    projectId: String,
    onBack   : () -> Unit,
    viewModel: GitHubViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    var tokenInput by remember { mutableStateOf("") }
    var showToken  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "GitHub Integration",
                subtitle = "Push your code to GitHub",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Step 1: Auth ──────────────────────────────────────────────────
            StepCard(step = 1, title = "Connect GitHub Account", isDone = uiState.user != null) {
                if (uiState.user != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(SecondaryPurple.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text("👤", fontSize = 20.sp) }
                        Column {
                            Text(uiState.user!!.login, fontWeight = FontWeight.Bold)
                            Text("${uiState.user!!.publicRepos} public repos", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Generate a Personal Access Token at github.com/settings/tokens with repo permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value         = tokenInput,
                            onValueChange = { tokenInput = it },
                            label         = { Text("GitHub Personal Access Token") },
                            leadingIcon   = { Icon(Icons.Default.Key, null) },
                            trailingIcon  = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                }
                            },
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier   = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        GradientButton(
                            text    = if (uiState.isLoading) "Connecting…" else "Connect GitHub",
                            onClick = { viewModel.authenticate(tokenInput) },
                            enabled  = tokenInput.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Step 2: Create Repo ───────────────────────────────────────────
            StepCard(step = 2, title = "Create Repository", isDone = uiState.createdRepo != null,
                enabled = uiState.user != null) {
                if (uiState.createdRepo != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                        Column {
                            Text(uiState.createdRepo!!.fullName, fontWeight = FontWeight.Bold)
                            Text(uiState.createdRepo!!.htmlUrl, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value         = uiState.repoName,
                            onValueChange = { viewModel.updateRepoName(it) },
                            label         = { Text("Repository Name") },
                            leadingIcon   = { Icon(Icons.Default.Folder, null) },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            enabled       = uiState.user != null
                        )
                        OutlinedTextField(
                            value         = uiState.repoDescription,
                            onValueChange = { viewModel.updateRepoDescription(it) },
                            label         = { Text("Description (optional)") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            enabled       = uiState.user != null
                        )
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Private repository", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = uiState.isPrivate, onCheckedChange = { viewModel.updateIsPrivate(it) },
                                enabled = uiState.user != null)
                        }
                        GradientButton(
                            text    = if (uiState.isLoading) "Creating…" else "Create Repository",
                            onClick = { viewModel.createRepo() },
                            enabled  = uiState.user != null && uiState.repoName.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Step 3: Push Code ─────────────────────────────────────────────
            StepCard(step = 3, title = "Push Code to GitHub", isDone = uiState.isPushSuccess,
                enabled = uiState.createdRepo != null) {
                if (uiState.isPushSuccess) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                        Column {
                            Text("Code pushed successfully!", fontWeight = FontWeight.Bold, color = SuccessGreen)
                            Text("All files uploaded to ${uiState.createdRepo?.fullName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("📱 frontend/main.js", "⚙️ backend/server.js", "🗄️ database/schema.sql",
                            "🐳 Dockerfile", "📄 README.md", "🧪 tests/app.test.js").forEach { file ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                Text(file, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        GradientButton(
                            text    = if (uiState.isPushing) "Pushing…" else "Push All Files 🚀",
                            onClick = { viewModel.pushCode(projectId) },
                            enabled  = uiState.createdRepo != null && !uiState.isPushing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Error
            uiState.errorMessage?.let { err ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(0.1f)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Error, null, tint = ErrorRed)
                    Text(err, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    step   : Int,
    title  : String,
    isDone : Boolean,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background((if (isDone) SuccessGreen else PrimaryBlue).copy(alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else Text(step.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
