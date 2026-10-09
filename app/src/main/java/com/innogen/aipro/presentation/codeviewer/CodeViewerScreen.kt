package com.innogen.aipro.presentation.codeviewer

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.domain.model.GeneratedCode
import com.innogen.aipro.presentation.GradientTopBar
import com.innogen.aipro.presentation.project.ProjectViewModel
import com.innogen.aipro.presentation.theme.PrimaryBlue

data class CodeFileInfo(
    val icon: String,
    val filename: String,
    val tabLabel: String,
    val content: String,
    val language: String,
    val isRunnableWeb: Boolean = false
)

enum class ViewMode { CODE, LIVE_PREVIEW }

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeViewerScreen(
    projectId: String,
    onBack   : () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFileIndex by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf(ViewMode.CODE) }
    val context = LocalContext.current

    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }

    val code = uiState.project?.generatedCode ?: GeneratedCode()

    val files = remember(code) {
        buildList {
            if (code.frontendCode.isNotBlank()) {
                add(CodeFileInfo("🌐", "index.html", "Frontend (HTML/JS)", code.frontendCode, "html", isRunnableWeb = true))
            }
            if (code.backendCode.isNotBlank()) {
                add(CodeFileInfo("⚙️", "server.js", "Backend API", code.backendCode, "javascript"))
            }
            if (code.databaseSchema.isNotBlank()) {
                add(CodeFileInfo("🗄️", "schema.sql", "SQL Database", code.databaseSchema, "sql"))
            }
            if (code.dockerConfig.isNotBlank()) {
                add(CodeFileInfo("🐳", "Dockerfile", "Dockerfile", code.dockerConfig, "dockerfile"))
            }
            if (code.readme.isNotBlank()) {
                add(CodeFileInfo("📄", "README.md", "README", code.readme, "markdown"))
            }
            if (code.apiDocs.isNotBlank()) {
                add(CodeFileInfo("📡", "API.md", "API Docs", code.apiDocs, "markdown"))
            }
            if (code.testCases.isNotBlank()) {
                add(CodeFileInfo("🧪", "test.js", "Test Suite", code.testCases, "javascript"))
            }
        }
    }

    val currentFile = files.getOrNull(selectedFileIndex)

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Code Viewer",
                subtitle = uiState.project?.title ?: "Multi-file Project",
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

            // ── Mode Switcher: Code vs Live Preview ────────────────────────────
            if (code.frontendCode.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E2333))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == ViewMode.CODE,
                        onClick = { viewMode = ViewMode.CODE },
                        label = { Text("💻 Source Files (${files.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF283046),
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                    FilterChip(
                        selected = viewMode == ViewMode.LIVE_PREVIEW,
                        onClick = { viewMode = ViewMode.LIVE_PREVIEW },
                        label = { Text("▶️ Run App (Live Preview)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF283046),
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            // ── Live Preview WebView Mode ──────────────────────────────────────
            if (viewMode == ViewMode.LIVE_PREVIEW) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadDataWithBaseURL(
                                    "https://localhost",
                                    code.frontendCode,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                "https://localhost",
                                code.frontendCode,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    )
                }
                return@Scaffold
            }

            // ── File selector tabs ────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedFileIndex,
                edgePadding      = 8.dp,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = PrimaryBlue
            ) {
                files.forEachIndexed { index, file ->
                    Tab(
                        selected = selectedFileIndex == index,
                        onClick  = { selectedFileIndex = index },
                        text     = {
                            Text(
                                "${file.icon} ${file.filename}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedFileIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Divider(color = Color(0xFF334155))

            // ── Code content ──────────────────────────────────────────────────
            if (currentFile != null) {
                val lines = remember(currentFile.content) { currentFile.content.lines() }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E222D))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text  = currentFile.filename,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF67E8F9)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF334155)
                            ) {
                                Text(
                                    text = "${lines.size} lines",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (currentFile.isRunnableWeb) {
                                Button(
                                    onClick = { viewMode = ViewMode.LIVE_PREVIEW },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("▶ Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(currentFile.filename, currentFile.content))
                                    Toast.makeText(context, "Copied ${currentFile.filename} to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Code area with line numbers
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Row {
                            // Line numbers column
                            val lineNumbersText = remember(lines.size) {
                                (1..lines.size).joinToString("\n")
                            }
                            Text(
                                text = lineNumbersText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF475569),
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            // Actual code text
                            Text(
                                text  = currentFile.content,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFFE2E8F0),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
