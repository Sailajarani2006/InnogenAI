package com.innogen.aipro.presentation.dashboard

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.domain.model.Project
import com.innogen.aipro.domain.model.ProjectStatus
import com.innogen.aipro.presentation.*
import com.innogen.aipro.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGenerateApp: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel    : DashboardViewModel = hiltViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    var prompt   by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    // Voice recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) prompt = spoken
    }

    // Permission for mic
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your app idea…")
            }
            voiceLauncher.launch(intent)
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { projectId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title   = { Text("Delete Project") },
            text    = { Text("Are you sure you want to delete this project? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProject(projectId)
                    showDeleteDialog = null
                }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text       = "Hello, ${uiState.userName} 👋",
                                    style      = MaterialTheme.typography.titleLarge,
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text  = "What will you build today?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            IconButton(onClick = onOpenProfile) {
                                Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Prompt input ──────────────────────────────────────
                        OutlinedTextField(
                            value         = prompt,
                            onValueChange = { prompt = it },
                            placeholder   = { Text("Describe your app idea…", color = Color.White.copy(0.7f)) },
                            trailingIcon  = {
                                Row {
                                    IconButton(onClick = {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }) {
                                        Icon(Icons.Default.Mic, "Voice", tint = Color.White)
                                    }
                                    IconButton(
                                        onClick  = { if (prompt.isNotBlank()) onGenerateApp(prompt) },
                                        enabled  = prompt.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.Send, "Generate", tint = Color.White)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor      = Color.White,
                                unfocusedTextColor    = Color.White,
                                focusedBorderColor    = Color.White,
                                unfocusedBorderColor  = Color.White.copy(0.5f),
                                cursorColor           = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            maxLines = 3
                        )
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader("Quick Actions")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon     = Icons.Default.AutoAwesome,
                            label    = "Generate App",
                            color    = PrimaryBlue,
                            modifier = Modifier.weight(1f),
                            onClick  = { if (prompt.isNotBlank()) onGenerateApp(prompt) }
                        )
                        QuickActionCard(
                            icon     = Icons.Default.GridView,
                            label    = "Templates",
                            color    = SecondaryPurple,
                            modifier = Modifier.weight(1f),
                            onClick  = { prompt = "E-commerce app with product catalog, cart, payments and user accounts" }
                        )
                        QuickActionCard(
                            icon     = Icons.Default.FolderOpen,
                            label    = "My Projects",
                            color    = AccentCyan,
                            modifier = Modifier.weight(1f),
                            onClick  = { /* scroll to projects */ }
                        )
                    }
                }
            }

            // ── Template suggestions ──────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader("Try These Ideas")
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                LazyRow(
                    contentPadding      = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templateIdeas) { idea ->
                        TemplateChip(idea = idea, onClick = { prompt = idea })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Recent Projects ───────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title    = "Recent Projects",
                        subtitle = "${uiState.projects.size} projects"
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else if (uiState.projects.isEmpty()) {
                item {
                    EmptyState(
                        icon     = Icons.Default.AutoAwesome,
                        title    = "No projects yet",
                        subtitle = "Describe an app idea above and let AI generate your first project!",
                        action   = {
                            GradientButton(
                                text    = "Generate My First App",
                                onClick = { onGenerateApp("A simple todo app with categories, reminders, and cloud sync") },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    )
                }
            } else {
                items(uiState.projects) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onOpenProject(project.id) },
                        onDelete = { showDeleteDialog = project.id },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionCard(
    icon    : ImageVector,
    label   : String,
    color   : Color,
    modifier: Modifier,
    onClick : () -> Unit
) {
    AppCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TemplateChip(idea: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, PrimaryBlue.copy(0.3f), RoundedCornerShape(50))
            .background(PrimaryBlue.copy(0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text  = idea,
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryBlue,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ProjectCard(
    project : Project,
    onClick : () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    AppCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Row(
                    modifier          = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text       = project.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1
                        )
                        Text(
                            text  = dateFormat.format(Date(project.updatedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        label = project.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = when (project.status) {
                            ProjectStatus.COMPLETE   -> SuccessGreen
                            ProjectStatus.GENERATING -> PrimaryBlue
                            ProjectStatus.ERROR      -> ErrorRed
                            else                     -> Color.Gray
                        }
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (project.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text     = project.description,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (project.techStack != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TechChip(project.techStack.frontend)
                    TechChip(project.techStack.backend)
                }
            }
        }
    }
}

@Composable
private fun TechChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

private val templateIdeas = listOf(
    "📱 Social media app",
    "🛒 E-commerce platform",
    "🎓 Learning management system",
    "💬 Real-time chat app",
    "🏥 Healthcare booking system",
    "📊 Analytics dashboard",
    "🍔 Food delivery app",
    "💼 Project management tool"
)
