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
import kotlinx.coroutines.launch
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
    var showTemplatesSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Refresh projects from Firestore on every screen display
    LaunchedEffect(Unit) {
        viewModel.refreshProjects()
    }

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
            text    = { Text("Are you sure you want to delete this project? This will delete it across web and mobile.") },
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

    // Templates Bottom Sheet
    if (showTemplatesSheet) {
        TemplatesBottomSheet(
            onDismiss = { showTemplatesSheet = false },
            onSelectTemplate = { selectedPrompt ->
                prompt = selectedPrompt
                showTemplatesSheet = false
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            onGenerateTemplate = { templatePrompt ->
                showTemplatesSheet = false
                onGenerateApp(templatePrompt)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state               = listState,
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
                                    text       = "Hello, ${uiState.userName.ifBlank { "Creator" }} 👋",
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
                            onClick  = {
                                if (prompt.isNotBlank()) {
                                    onGenerateApp(prompt)
                                } else {
                                    coroutineScope.launch { listState.animateScrollToItem(0) }
                                }
                            }
                        )
                        QuickActionCard(
                            icon     = Icons.Default.GridView,
                            label    = "Templates",
                            color    = SecondaryPurple,
                            modifier = Modifier.weight(1f),
                            onClick  = { showTemplatesSheet = true }
                        )
                        QuickActionCard(
                            icon     = Icons.Default.FolderOpen,
                            label    = "My Projects",
                            color    = AccentCyan,
                            modifier = Modifier.weight(1f),
                            onClick  = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(3)
                                }
                            }
                        )
                    }
                }
            }

            // ── Template suggestions ──────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Try These Ideas")
                        TextButton(onClick = { showTemplatesSheet = true }) {
                            Text("See All", color = PrimaryBlue, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                                .clickable { showTemplatesSheet = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.GridView, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Browse Templates", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    items(templateIdeas) { idea ->
                        TemplateChip(idea = idea, onClick = { prompt = idea })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Recent Projects ───────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title    = "Recent Projects",
                            subtitle = "${uiState.projects.size} projects"
                        )
                        IconButton(onClick = { viewModel.refreshProjects() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (uiState.isLoading && uiState.projects.isEmpty()) {
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
                        subtitle = "Describe an app idea above or choose from our templates to generate your first project!",
                        action   = {
                            GradientButton(
                                text    = "Explore Templates",
                                onClick = { showTemplatesSheet = true },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    )
                }
            } else {
                items(uiState.projects, key = { it.id }) { project ->
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

// ── Templates Bottom Sheet ────────────────────────────────────────────────────

data class AppTemplate(
    val id         : String,
    val name       : String,
    val description: String,
    val prompt     : String,
    val icon       : ImageVector,
    val color      : Color,
    val category   : String,
    val tags       : List<String>
)

val appTemplatesList = listOf(
    AppTemplate(
        id          = "ecommerce",
        name        = "E-Commerce Store",
        description = "Full online store with catalog, shopping cart, checkout & payment processing",
        prompt      = "E-Commerce store with product catalog, shopping cart, Stripe payment gateway, order tracking and customer accounts",
        icon        = Icons.Default.ShoppingCart,
        color       = Color(0xFF3B82F6),
        category    = "Commerce",
        tags        = listOf("Store", "Payments", "Cart")
    ),
    AppTemplate(
        id          = "lms",
        name        = "LMS Learning Platform",
        description = "Courses, video lectures, student enrollment, quizzes & progress tracking",
        prompt      = "LMS Learning Management platform with course catalog, video lectures, student enrollment, quizzes, progress tracking and certificates",
        icon        = Icons.Default.School,
        color       = Color(0xFF8B5CF6),
        category    = "Education",
        tags        = listOf("Courses", "Quizzes", "Students")
    ),
    AppTemplate(
        id          = "chat",
        name        = "Real-Time Chat App",
        description = "1-on-1 direct messages, group channels, active status & file sharing",
        prompt      = "Real-time chat and messaging application with direct messaging, group channels, online presence, media attachments and push notifications",
        icon        = Icons.Default.Chat,
        color       = Color(0xFF10B981),
        category    = "Social",
        tags        = listOf("Chat", "Realtime", "Channels")
    ),
    AppTemplate(
        id          = "health",
        name        = "Health & Fitness Tracker",
        description = "Workout plans, step counter, calorie logs & daily wellness statistics",
        prompt      = "Health and fitness tracker app with daily workout plans, step counter, calorie logging, water reminder and wellness analytics",
        icon        = Icons.Default.Favorite,
        color       = Color(0xFFF43F5E),
        category    = "Health",
        tags        = listOf("Fitness", "Calories", "Wellness")
    ),
    AppTemplate(
        id          = "news",
        name        = "News & Content Portal",
        description = "Category article feeds, breaking news alerts, bookmarks & comments",
        prompt      = "Modern news and media app with category feeds, breaking news alerts, bookmarking, article search and reader comments",
        icon        = Icons.Default.Article,
        color       = Color(0xFFF59E0B),
        category    = "Content",
        tags        = listOf("News", "Articles", "Feeds")
    ),
    AppTemplate(
        id          = "music",
        name        = "Music Player & Streamer",
        description = "Track library, playlist manager, audio controls & artist exploration",
        prompt      = "Music streaming and audio player app with song library, playlist management, album artwork, audio player controls and search",
        icon        = Icons.Default.MusicNote,
        color       = Color(0xFFEC4899),
        category    = "Media",
        tags        = listOf("Audio", "Playlists", "Streaming")
    ),
    AppTemplate(
        id          = "tasks",
        name        = "Project & Task Kanban",
        description = "Agile Kanban boards, task priorities, deadlines & team collaboration",
        prompt      = "Project management and task tracker with Kanban boards, priority tags, due date reminders, subtasks and team collaboration",
        icon        = Icons.Default.CheckCircle,
        color       = Color(0xFF06B6D4),
        category    = "Productivity",
        tags        = listOf("Kanban", "Agile", "Tasks")
    ),
    AppTemplate(
        id          = "social",
        name        = "Social Media Network",
        description = "Photo & text posts, interactive feed, likes, comments & profiles",
        prompt      = "Social media platform with user feed, post creation with images, likes, threaded comments, follower system and user profiles",
        icon        = Icons.Default.Share,
        color       = Color(0xFF6366F1),
        category    = "Social",
        tags        = listOf("Feed", "Posts", "Profiles")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesBottomSheet(
    onDismiss         : () -> Unit,
    onSelectTemplate  : (String) -> Unit,
    onGenerateTemplate: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Commerce", "Education", "Social", "Health", "Content", "Media", "Productivity")

    val filteredTemplates = remember(selectedCategory) {
        if (selectedCategory == "All") appTemplatesList
        else appTemplatesList.filter { it.category == selectedCategory }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.GridView, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text       = "App Templates",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text  = "Pick a template to build or customize instantly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Template cards list
            LazyColumn(
                modifier            = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredTemplates, key = { it.id }) { template ->
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(template.color, template.color.copy(alpha = 0.75f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(template.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text       = template.name,
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        StatusBadge(label = template.category, color = template.color)
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text  = template.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        template.tags.forEach { tag ->
                                            TechChip(tag)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(10.dp))

                            // Action buttons
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick  = { onSelectTemplate(template.prompt) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape    = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Use Prompt", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick  = { onGenerateTemplate(template.prompt) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape    = RoundedCornerShape(10.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = template.color)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Generate", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }
                        }
                    }
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
