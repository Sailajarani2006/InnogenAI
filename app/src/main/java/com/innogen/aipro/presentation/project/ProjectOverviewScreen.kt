package com.innogen.aipro.presentation.project

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.domain.model.*
import com.innogen.aipro.presentation.*
import com.innogen.aipro.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectOverviewScreen(
    projectId : String,
    onViewCode: () -> Unit,
    onDeploy  : () -> Unit,
    onGitHub  : () -> Unit,
    onBack    : () -> Unit,
    viewModel : ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }

    var selectedTab    by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText       by remember { mutableStateOf("") }

    val tabs = listOf("Overview", "Code", "Bugs", "Security", "Tests", "Edit")

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title   = { Text("Modify & Regenerate") },
            text    = {
                OutlinedTextField(
                    value         = editText,
                    onValueChange = { editText = it },
                    placeholder   = { Text("E.g. Add payment gateway, add dark mode, add push notifications…") },
                    modifier      = Modifier.fillMaxWidth().height(120.dp)
                )
            },
            confirmButton = {
                GradientButton(
                    text    = "Regenerate",
                    onClick = {
                        viewModel.regenerate(editText)
                        showEditDialog = false
                    },
                    enabled  = editText.isNotBlank(),
                    modifier = Modifier.width(140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title    = uiState.project?.title ?: "Project",
                subtitle = uiState.project?.description,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.project == null) {
            EmptyState(icon = Icons.Default.Error, title = "Project not found", subtitle = "Please go back and try again")
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                // ── Quick Actions Row ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton("View Code", Icons.Default.Code, PrimaryBlue, Modifier.weight(1f), onViewCode)
                    ActionButton("GitHub", Icons.Default.Share, SecondaryPurple, Modifier.weight(1f), onGitHub)
                    ActionButton("Deploy", Icons.Default.RocketLaunch, AccentCyan, Modifier.weight(1f), onDeploy)
                }

                // ── Tab Row ───────────────────────────────────────────────────
                ScrollableTabRow(
                    selectedTabIndex  = selectedTab,
                    edgePadding       = 16.dp,
                    containerColor    = MaterialTheme.colorScheme.surface,
                    contentColor      = PrimaryBlue
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text     = { Text(title, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                // ── Tab Content ───────────────────────────────────────────────
                when (selectedTab) {
                    0 -> OverviewTab(uiState.project!!)
                    1 -> CodeSummaryTab(uiState.project!!, onViewCode)
                    2 -> BugsTab(uiState, viewModel)
                    3 -> SecurityTab(uiState, viewModel)
                    4 -> TestsTab(uiState, viewModel)
                    5 -> EditTab(uiState) { viewModel.regenerate(it) }
                }
            }
        }
    }
}

// ── Tab: Overview ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(project: Project) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard("📋 Description", project.description)
        }
        item {
            if (project.features.isNotEmpty()) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✨ Features", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        project.features.forEach { feature ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                                Text("•", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                Text(feature, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        item {
            project.techStack?.let { ts ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🛠 Tech Stack", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TechRow("Frontend", ts.frontend, PrimaryBlue)
                        TechRow("Backend",  ts.backend,  SecondaryPurple)
                        TechRow("Database", ts.database, AccentCyan)
                        TechRow("Deploy",   ts.deployment, SuccessGreen)
                    }
                }
            }
        }
    }
}

// ── Tab: Code summary ─────────────────────────────────────────────────────────

@Composable
private fun CodeSummaryTab(project: Project, onViewFullCode: () -> Unit) {
    val code = project.generatedCode
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (code != null) {
            item { CodePreviewCard("frontend/", "Frontend Code", code.frontendCode) }
            item { CodePreviewCard("backend/",  "Backend Code",  code.backendCode) }
            item { CodePreviewCard("database/", "DB Schema",     code.databaseSchema) }
        }
        item {
            GradientButton("View Full Code", onViewFullCode, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Tab: Bugs ─────────────────────────────────────────────────────────────────

@Composable
private fun BugsTab(state: ProjectUiState, viewModel: ProjectViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.bugs.isEmpty()) {
            item {
                EmptyState(
                    icon     = Icons.Default.BugReport,
                    title    = "No bugs detected yet",
                    subtitle = "Run AI bug detection to scan your generated code",
                    action   = {
                        GradientButton(
                            text    = if (state.isAnalyzing) "Scanning…" else "Detect Bugs 🐞",
                            onClick = { viewModel.detectBugs() },
                            enabled  = !state.isAnalyzing,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                )
            }
        } else {
            items(state.bugs) { bug ->
                BugCard(bug)
            }
        }
    }
}

// ── Tab: Security ─────────────────────────────────────────────────────────────

@Composable
private fun SecurityTab(state: ProjectUiState, viewModel: ProjectViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.securityIssues.isEmpty()) {
            item {
                EmptyState(
                    icon     = Icons.Default.Security,
                    title    = "Security not analyzed yet",
                    subtitle = "Run AI security analysis to find vulnerabilities",
                    action   = {
                        GradientButton(
                            text    = if (state.isAnalyzing) "Analyzing…" else "Analyze Security 🔐",
                            onClick = { viewModel.analyzeSecurity() },
                            enabled  = !state.isAnalyzing,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                )
            }
        } else {
            items(state.securityIssues) { issue ->
                SecurityIssueCard(issue)
            }
        }
    }
}

// ── Tab: Tests ────────────────────────────────────────────────────────────────

@Composable
private fun TestsTab(state: ProjectUiState, viewModel: ProjectViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.testCases.isEmpty()) {
            item {
                EmptyState(
                    icon     = Icons.Default.Science,
                    title    = "No tests generated yet",
                    subtitle = "Generate AI test cases for your project",
                    action   = {
                        GradientButton(
                            text    = if (state.isAnalyzing) "Generating…" else "Generate Tests 🧪",
                            onClick = { viewModel.generateTests() },
                            enabled  = !state.isAnalyzing,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                )
            }
        } else {
            items(state.testCases) { tc ->
                TestCaseCard(tc)
            }
        }
    }
}

// ── Tab: Edit / Regenerate ────────────────────────────────────────────────────

@Composable
private fun EditTab(state: ProjectUiState, onRegenerate: (String) -> Unit) {
    var instruction by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("🔄 Modify & Regenerate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Describe changes to make to your app. AI will update the complete code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Add payment gateway with Stripe",
                "Add push notifications",
                "Add dark mode support",
                "Add authentication with OAuth"
            ).forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PrimaryBlue.copy(0.3f), RoundedCornerShape(10.dp))
                        .clickable { instruction = suggestion }
                        .padding(12.dp)
                ) {
                    Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = PrimaryBlue)
                }
            }
        }

        OutlinedTextField(
            value         = instruction,
            onValueChange = { instruction = it },
            placeholder   = { Text("Or type your own modification…") },
            modifier      = Modifier.fillMaxWidth().height(120.dp)
        )

        GradientButton(
            text    = if (state.isRegenerating) "Regenerating…" else "Regenerate App 🤖",
            onClick = { onRegenerate(instruction) },
            enabled  = instruction.isNotBlank() && !state.isRegenerating,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun InfoCard(label: String, content: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TechRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusBadge(value, color)
    }
}

@Composable
private fun CodePreviewCard(path: String, title: String, code: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(12.dp)
            ) {
                Text(
                    text     = code.take(200) + if (code.length > 200) "\n…" else "",
                    style    = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color    = Color(0xFFD4D4D4),
                    maxLines = 6
                )
            }
        }
    }
}

@Composable
private fun BugCard(bug: BugReport) {
    val color = when (bug.severity) {
        BugSeverity.CRITICAL -> ErrorRed
        BugSeverity.HIGH     -> Color(0xFFFF6B35)
        BugSeverity.MEDIUM   -> WarningOrange
        BugSeverity.LOW      -> SuccessGreen
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Line ${bug.line}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                StatusBadge(bug.severity.name, color)
            }
            Text(bug.description, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreen.copy(0.1f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Text(bug.fix, style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
            }
        }
    }
}

@Composable
private fun SecurityIssueCard(issue: SecurityIssue) {
    val color = when (issue.risk) {
        RiskLevel.CRITICAL -> ErrorRed
        RiskLevel.HIGH     -> Color(0xFFFF6B35)
        RiskLevel.MEDIUM   -> WarningOrange
        RiskLevel.LOW      -> SuccessGreen
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(issue.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                StatusBadge(issue.risk.name, color)
            }
            Text(issue.description, style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryBlue.copy(0.08f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Shield, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                Text(issue.mitigation, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
            }
        }
    }
}

@Composable
private fun TestCaseCard(tc: TestCase) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusBadge(tc.type.name, AccentCyan)
            }
            Text(tc.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(10.dp)
            ) {
                Text(
                    text  = tc.code,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = Color(0xFFD4D4D4),
                    maxLines = 5
                )
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
        }
    }
}
