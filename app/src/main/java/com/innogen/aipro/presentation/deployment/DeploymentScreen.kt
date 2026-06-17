package com.innogen.aipro.presentation.deployment

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
import com.innogen.aipro.presentation.*
import com.innogen.aipro.presentation.project.ProjectViewModel
import com.innogen.aipro.presentation.theme.*

@Composable
fun DeploymentScreen(
    projectId: String,
    onBack   : () -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(projectId) { viewModel.loadProject(projectId) }

    val dockerConfig = uiState.project?.generatedCode?.dockerConfig ?: defaultDockerConfig
    var expandedSection by remember { mutableStateOf<String?>("docker") }

    Scaffold(
        topBar = {
            GradientTopBar(
                title    = "Deployment",
                subtitle = "Export & deploy your project",
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

            // ── Header card ───────────────────────────────────────────────────
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚀", fontSize = 40.sp)
                    Column {
                        Text("Ready to Deploy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            uiState.project?.title ?: "Your App",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Deploy Steps ──────────────────────────────────────────────────
            Text("Deployment Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            deploySteps.forEachIndexed { index, step ->
                DeployStepCard(
                    step    = step,
                    number  = index + 1
                )
            }

            // ── Docker Config ─────────────────────────────────────────────────
            ExpandableSection(
                title      = "🐳 Dockerfile",
                isExpanded = expandedSection == "docker",
                onToggle   = { expandedSection = if (expandedSection == "docker") null else "docker" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(12.dp)
                    ) {
                        Text(
                            text  = dockerConfig,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFD4D4D4)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("dockerfile", dockerConfig))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            // ── Docker Commands ───────────────────────────────────────────────
            ExpandableSection(
                title      = "⌨️ Docker Commands",
                isExpanded = expandedSection == "commands",
                onToggle   = { expandedSection = if (expandedSection == "commands") null else "commands" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dockerCommands.forEach { (label, cmd) ->
                        CommandRow(label = label, command = cmd, context = context)
                    }
                }
            }

            // ── Cloud platforms ───────────────────────────────────────────────
            ExpandableSection(
                title      = "☁️ Cloud Platforms",
                isExpanded = expandedSection == "cloud",
                onToggle   = { expandedSection = if (expandedSection == "cloud") null else "cloud" }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cloudPlatforms.forEach { platform ->
                        CloudPlatformCard(platform)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeployStepCard(step: DeployStep, number: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(PrimaryBlue.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = PrimaryBlue, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(step.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(step.emoji, fontSize = 20.sp)
    }
}

@Composable
private fun ExpandableSection(
    title     : String,
    isExpanded: Boolean,
    onToggle  : () -> Unit,
    content   : @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier  = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun CommandRow(label: String, command: String, context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E1E))
                .clickable {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("cmd", command))
                }
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(command, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF9CDCFE), modifier = Modifier.weight(1f))
            Icon(Icons.Default.ContentCopy, "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CloudPlatformCard(platform: CloudPlatform) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(platform.emoji, fontSize = 28.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(platform.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(platform.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusBadge(platform.difficulty, platform.difficultyColor)
    }
}

// ── Data ──────────────────────────────────────────────────────────────────────

private data class DeployStep(val title: String, val description: String, val emoji: String)
private data class CloudPlatform(val name: String, val description: String, val emoji: String,
    val difficulty: String, val difficultyColor: Color)

private val deploySteps = listOf(
    DeployStep("Download Project Files", "Export all generated code to your machine", "📥"),
    DeployStep("Install Dependencies", "Run npm install or pip install -r requirements.txt", "📦"),
    DeployStep("Configure Environment", "Set up .env file with your API keys and DB credentials", "⚙️"),
    DeployStep("Build Docker Image", "Run: docker build -t my-app .", "🐳"),
    DeployStep("Push to Registry", "Run: docker push your-registry/my-app", "📤"),
    DeployStep("Deploy to Cloud", "Use your preferred cloud platform to run the container", "☁️")
)

private val dockerCommands = listOf(
    "Build image"     to "docker build -t my-app .",
    "Run locally"     to "docker run -p 3000:3000 my-app",
    "Stop container"  to "docker stop \$(docker ps -q)",
    "View logs"       to "docker logs -f \$(docker ps -q)",
    "Push to Hub"     to "docker push username/my-app:latest"
)

private val cloudPlatforms = listOf(
    CloudPlatform("Railway", "Deploy in seconds with automatic HTTPS", "🚂", "Easy", SuccessGreen),
    CloudPlatform("Render", "Free tier available, auto-deploy from GitHub", "🔷", "Easy", SuccessGreen),
    CloudPlatform("AWS ECS", "Enterprise-grade container orchestration", "☁️", "Advanced", WarningOrange),
    CloudPlatform("Google Cloud Run", "Serverless containers, pay per use", "🌐", "Medium", PrimaryBlue),
    CloudPlatform("DigitalOcean", "Simple VPS with Docker support", "🌊", "Medium", PrimaryBlue)
)

private val defaultDockerConfig = """
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
EXPOSE 3000
ENV NODE_ENV=production
CMD ["node", "server.js"]
""".trimIndent()
