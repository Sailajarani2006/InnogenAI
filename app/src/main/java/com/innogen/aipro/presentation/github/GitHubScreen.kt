package com.innogen.aipro.presentation.github

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    // Show success snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GradientTopBar(
                title    = "GitHub Integration",
                subtitle = if (uiState.isConnected) "Connected as ${uiState.user?.login}" else "Connect your GitHub account",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (uiState.isConnected) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.LinkOff, "Disconnect", tint = Color.White)
                        }
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

            // ── Step 1: Connect GitHub ────────────────────────────────────────
            StepCard(
                step    = 1,
                title   = "Connect GitHub Account",
                isDone  = uiState.isConnected,
                enabled = true
            ) {
                if (uiState.isConnected && uiState.user != null) {
                    // Connected state
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                uiState.user.login.first().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiState.user.name ?: uiState.user.login,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "@${uiState.user.login} · ${uiState.user.publicRepos} repos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick  = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(0.5f))
                    ) {
                        Icon(Icons.Default.LinkOff, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Disconnect Account")
                    }
                } else {
                    // Not connected state
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (uiState.deviceCodeData != null) {
                            val codeData = uiState.deviceCodeData
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            var isCopied by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E1B4B).copy(alpha = 0.5f))
                                    .border(1.dp, Brush.linearGradient(listOf(GradientStart, GradientEnd)), RoundedCornerShape(14.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "1. Copy your verification code:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .clickable {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(codeData.userCode))
                                            isCopied = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = codeData.userCode,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA78BFA),
                                        letterSpacing = 2.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            null,
                                            tint = if (isCopied) SuccessGreen else Color.White.copy(0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            if (isCopied) "Copied!" else "Copy",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isCopied) SuccessGreen else Color.White.copy(0.7f)
                                        )
                                    }
                                }

                                Text(
                                    "2. Log in and authorize on GitHub:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )

                                Button(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(codeData.userCode))
                                        isCopied = true
                                        val uri = Uri.parse(codeData.verificationUri)
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("🐙 Open GitHub to Authorize", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Waiting for authorization on GitHub...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            // Device Flow button (primary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF24292F))
                                    .clickable {
                                        viewModel.startDeviceAuth()
                                    }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("🐙", fontSize = 22.sp)
                                    Column {
                                        Text(
                                            "Sign in with GitHub",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "Official GitHub login — no token needed",
                                            color = Color.White.copy(0.6f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(modifier = Modifier.weight(1f))
                            Text("or enter token manually", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Divider(modifier = Modifier.weight(1f))
                        }

                        // Manual token fallback
                        var manualToken by remember { mutableStateOf("") }
                        var showToken   by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value         = manualToken,
                            onValueChange = { manualToken = it },
                            label         = { Text("Personal Access Token") },
                            leadingIcon   = { Icon(Icons.Default.Key, null) },
                            trailingIcon  = {
                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null
                                    )
                                }
                            },
                            visualTransformation = if (showToken) androidx.compose.ui.text.input.VisualTransformation.None
                                                   else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier   = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("ghp_xxxxxxxxxxxx") }
                        )
                        OutlinedButton(
                            onClick  = { if (manualToken.isNotBlank()) viewModel.handleOAuthCallback(manualToken) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled  = manualToken.isNotBlank() && !uiState.isLoading
                        ) {
                            Text("Connect with Token")
                        }
                    }
                }

                if (uiState.isLoading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
                }
            }

            // ── Step 2: Create Repository ─────────────────────────────────────
            StepCard(
                step    = 2,
                title   = "Create Repository",
                isDone  = uiState.createdRepo != null,
                enabled = uiState.isConnected
            ) {
                if (uiState.createdRepo != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                        Column {
                            Text(uiState.createdRepo.fullName, fontWeight = FontWeight.Bold)
                            Text(
                                uiState.createdRepo.htmlUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryBlue
                            )
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
                            enabled       = uiState.isConnected
                        )
                        OutlinedTextField(
                            value         = uiState.repoDescription,
                            onValueChange = { viewModel.updateRepoDescription(it) },
                            label         = { Text("Description (optional)") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            enabled       = uiState.isConnected
                        )
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Private Repository", style = MaterialTheme.typography.bodyMedium)
                                Text("Only you can see this repo", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked         = uiState.isPrivate,
                                onCheckedChange = { viewModel.updateIsPrivate(it) },
                                enabled         = uiState.isConnected
                            )
                        }
                        GradientButton(
                            text     = if (uiState.isLoading) "Creating…" else "Create Repository",
                            onClick  = { viewModel.createRepo() },
                            enabled  = uiState.isConnected && uiState.repoName.isNotBlank() && !uiState.isLoading,
                            icon     = Icons.Default.Add,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Step 3: Push Code ─────────────────────────────────────────────
            StepCard(
                step    = 3,
                title   = "Push Code to GitHub",
                isDone  = uiState.isPushSuccess,
                enabled = uiState.createdRepo != null
            ) {
                if (uiState.isPushSuccess) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                            Column {
                                Text("Code pushed successfully! 🎉", fontWeight = FontWeight.Bold, color = SuccessGreen)
                                Text(
                                    "View at: ${uiState.createdRepo?.htmlUrl}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick  = {
                                uiState.createdRepo?.htmlUrl?.let { url ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Repository on GitHub")
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Files to be pushed
                        listOf(
                            "📱 frontend/main.js",
                            "⚙️ backend/server.js",
                            "🗄️ database/schema.sql",
                            "🐳 Dockerfile",
                            "📄 README.md",
                            "📡 docs/API.md",
                            "🧪 tests/app.test.js"
                        ).forEach { file ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.InsertDriveFile, null,
                                    tint     = PrimaryBlue,
                                    modifier = Modifier.size(16.dp))
                                Text(file, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (uiState.isPushing) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Pushing files...", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            GradientButton(
                                text     = "Push All Files 🚀",
                                onClick  = { viewModel.pushCode(projectId) },
                                enabled  = uiState.createdRepo != null && !uiState.isPushing,
                                icon     = Icons.Default.Upload,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ── Error message ─────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(0.1f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Text(
                        uiState.errorMessage ?: "",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
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
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone  -> SuccessGreen
                                enabled -> PrimaryBlue
                                else    -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            step.toString(),
                            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = if (enabled) MaterialTheme.colorScheme.onSurface
                                 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (enabled) content()
        }
    }
}
