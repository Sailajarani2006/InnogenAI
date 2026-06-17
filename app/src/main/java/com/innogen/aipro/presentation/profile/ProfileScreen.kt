package com.innogen.aipro.presentation.profile

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.innogen.aipro.presentation.*
import com.innogen.aipro.presentation.theme.*

@Composable
fun ProfileScreen(
    onBack    : () -> Unit,
    onSignOut : () -> Unit,
    viewModel : ProfileViewModel = hiltViewModel()
) {
    val isDark       by viewModel.isDarkMode.collectAsState(false)
    val projectCount by viewModel.projectCount.collectAsState()
    val user         = viewModel.getCurrentUser()

    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title   = { Text("Sign Out") },
            text    = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.signOut()
                    showSignOutDialog = false
                    onSignOut()
                }) { Text("Sign Out", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title    = "Profile",
                subtitle = "Settings & preferences",
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

            // ── User card ─────────────────────────────────────────────────────
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier         = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text     = user?.name?.firstOrNull()?.uppercase() ?: "U",
                                color    = Color.White,
                                style    = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = user?.name ?: "User",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text  = user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusBadge("Free Plan", PrimaryBlue)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    // Stats
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Projects", value = projectCount.toString(), emoji = "📱")
                        StatItem(label = "API Calls", value = "∞", emoji = "⚡")
                        StatItem(label = "Plan", value = "Free", emoji = "🆓")
                    }
                }
            }

            // ── Settings ──────────────────────────────────────────────────────
            Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsToggleRow(
                        icon    = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        label   = "Dark Mode",
                        checked = isDark,
                        onToggle = { viewModel.toggleDarkMode() }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Notifications, "Push Notifications", "Manage alerts")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Language, "Language", "English")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Storage, "Storage", "Clear local cache")
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(Icons.Default.Info, "App Version", "1.0.0")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Policy, "Privacy Policy", "Read our policy")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.Article, "Terms of Service", "Read terms")
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(Icons.Default.BugReport, "Report a Bug", "Help us improve")
                }
            }

            // ── Sign Out ──────────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border   = BorderStroke(1.dp, ErrorRed)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text  = "InnoGen AI Pro v1.0.0\nPowered by GPT-4o",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, emoji: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue, checkedTrackColor = PrimaryBlue.copy(0.3f))
        )
    }
}
