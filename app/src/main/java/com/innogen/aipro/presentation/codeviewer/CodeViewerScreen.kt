package com.innogen.aipro.presentation.codeviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.domain.model.GeneratedCode
import com.innogen.aipro.presentation.GradientTopBar
import com.innogen.aipro.presentation.project.ProjectViewModel
import com.innogen.aipro.presentation.theme.PrimaryBlue

data class CodeFile(val icon: String, val label: String, val content: String, val language: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeViewerScreen(
    projectId: String,
    onBack   : () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFile by remember { mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }

    val code = uiState.project?.generatedCode ?: GeneratedCode()

    val files = remember(code) {
        buildList {
            if (code.frontendCode.isNotBlank())   add(CodeFile("📱", "Frontend",    code.frontendCode,    "javascript"))
            if (code.backendCode.isNotBlank())    add(CodeFile("⚙️", "Backend",     code.backendCode,     "javascript"))
            if (code.databaseSchema.isNotBlank()) add(CodeFile("🗄️", "Database",    code.databaseSchema,  "sql"))
            if (code.dockerConfig.isNotBlank())   add(CodeFile("🐳", "Dockerfile",  code.dockerConfig,    "dockerfile"))
            if (code.readme.isNotBlank())         add(CodeFile("📄", "README",      code.readme,          "markdown"))
            if (code.apiDocs.isNotBlank())        add(CodeFile("📡", "API Docs",    code.apiDocs,         "markdown"))
            if (code.testCases.isNotBlank())      add(CodeFile("🧪", "Tests",       code.testCases,       "javascript"))
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Code Viewer",
                subtitle = uiState.project?.title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                return@Scaffold
            }

            // ── File selector tabs ────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedFile,
                edgePadding      = 8.dp,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = PrimaryBlue
            ) {
                files.forEachIndexed { index, file ->
                    Tab(
                        selected = selectedFile == index,
                        onClick  = { selectedFile = index },
                        text     = {
                            Text(
                                "${file.icon} ${file.label}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            Divider()

            // ── Code content ──────────────────────────────────────────────────
            val currentFile = files.getOrNull(selectedFile)
            if (currentFile != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D2D))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "${currentFile.label}.${currentFile.language}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF9CDCFE)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("code", currentFile.content))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Code area (scrollable)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text  = currentFile.content,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFD4D4D4),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
