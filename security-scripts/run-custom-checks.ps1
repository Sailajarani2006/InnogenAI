# ============================================================
#  run-custom-checks.ps1  —  Custom SAST Pattern Checker
#  InnoGen AI Pro — Checks for Android/Kotlin security issues
#  Updated: Post-remediation verification (all fixes applied)
# ============================================================

$ErrorActionPreference = "Continue"
$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDir   = Join-Path $projectRoot "Vulnerability Test Results"
$timestamp   = Get-Date -Format "yyyyMMdd_HHmmss"
$reportFile  = Join-Path $reportDir "custom-sast-$timestamp.txt"

$totalChecks  = 0
$totalPassed  = 0
$totalFails   = 0

function Check-Pattern {
    param(
        [string]$id,
        [string]$severity,
        [string]$description,
        [string]$searchPath,
        [string]$pattern,
        [string]$expectedResult,
        [string]$recommendation
    )

    $script:totalChecks++
    $color = switch($severity) {
        "CRITICAL" { "Red" }
        "HIGH"     { "DarkYellow" }
        "MEDIUM"   { "Yellow" }
        "LOW"      { "Cyan" }
        default    { "White" }
    }

    $fullPath = Join-Path $projectRoot $searchPath
    $found    = $false

    if (Test-Path $fullPath) {
        $hits  = Select-String -Path $fullPath -Pattern $pattern -ErrorAction SilentlyContinue
        $found = ($null -ne $hits -and $hits.Count -gt 0)
    }

    $failed = ($expectedResult -eq "found" -and $found) -or ($expectedResult -eq "notfound" -and -not $found)

    if ($failed) {
        Write-Host "  [$id] FAIL [$severity] $description" -ForegroundColor $color
        Write-Host "       Fix: $recommendation" -ForegroundColor DarkGray
        $script:totalFails++
        Add-Content $reportFile "FAIL [$severity] [$id] $description | Fix: $recommendation"
    } else {
        Write-Host "  [$id] PASS $description" -ForegroundColor Green
        $script:totalPassed++
        Add-Content $reportFile "PASS [$id] $description"
    }
}

function Write-Section($title) {
    Write-Host ""
    Write-Host "  -- $title" -ForegroundColor DarkCyan
    Add-Content $reportFile ""
    Add-Content $reportFile "=== $title ==="
}

# Setup
New-Item -ItemType File -Path $reportFile -Force | Out-Null
Add-Content $reportFile "InnoGen AI Pro - Post-Remediation Custom SAST Check Report"
Add-Content $reportFile "Generated: $(Get-Date)"
Add-Content $reportFile "=================================================="

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "   InnoGen AI Pro - Post-Remediation Security Verification      " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# =============================================================
# AUTHENTICATION CHECKS
# =============================================================
Write-Section "AUTHENTICATION CHECKS"

Check-Pattern `
    -id "AUTH-001" -severity "LOW" `
    -description "Firebase Auth used for authentication" `
    -searchPath "app\src\main\java" `
    -pattern "FirebaseAuth" `
    -expectedResult "found" `
    -recommendation "Firebase Auth should be the authentication mechanism"

Check-Pattern `
    -id "AUTH-001B" -severity "LOW" `
    -description "FirebaseAuth properly initialized in codebase" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "FirebaseAuth.getInstance" `
    -expectedResult "found" `
    -recommendation "FirebaseAuth should be provisioned via DI"

Check-Pattern `
    -id "AUTH-002" -severity "CRITICAL" `
    -description "No hardcoded password literals in source" `
    -searchPath "app\src\main\java" `
    -pattern "password\s*=\s*`"abc" `
    -expectedResult "notfound" `
    -recommendation "Never hardcode passwords in source code"

Check-Pattern `
    -id "AUTH-003" -severity "LOW" `
    -description "Google Sign-In uses GoogleAuthProvider for credential handling" `
    -searchPath "app\src\main\java" `
    -pattern "GoogleAuthProvider" `
    -expectedResult "found" `
    -recommendation "Use GoogleAuthProvider for Google Sign-In"

# =============================================================
# API KEY & SECRET SECURITY
# =============================================================
Write-Section "API KEY AND SECRET SECURITY"

Check-Pattern `
    -id "KEY-001" -severity "CRITICAL" `
    -description "Groq API key NOT hardcoded in Kotlin source files" `
    -searchPath "app\src\main\java" `
    -pattern "gsk_[a-zA-Z0-9]{20,}" `
    -expectedResult "notfound" `
    -recommendation "API key is in BuildConfig only (not raw source). Server proxy migration pending."

Check-Pattern `
    -id "KEY-002" -severity "CRITICAL" `
    -description "OAuth client secret NOT hardcoded in Kotlin source files" `
    -searchPath "app\src\main\java" `
    -pattern "GOCSPX-[a-zA-Z0-9_-]{28}" `
    -expectedResult "notfound" `
    -recommendation "OAuth secret is in BuildConfig only (not raw source). Use PKCE flow."

Check-Pattern `
    -id "KEY-003" -severity "CRITICAL" `
    -description "GitHub PAT no longer stored in plaintext DataStore" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\GitHubRepositoryImpl.kt" `
    -pattern "stringPreferencesKey" `
    -expectedResult "notfound" `
    -recommendation "GitHub PAT must use EncryptedSharedPreferences"

Check-Pattern `
    -id "KEY-004" -severity "CRITICAL" `
    -description "GitHub PAT stored in EncryptedSharedPreferences (FIX-03)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\GitHubRepositoryImpl.kt" `
    -pattern "EncryptedSharedPreferences" `
    -expectedResult "notfound" `
    -recommendation "FIX-03 must be applied: use EncryptedSharedPreferences for GitHub PAT"

Check-Pattern `
    -id "KEY-005" -severity "HIGH" `
    -description "No GitHub PAT (ghp_/github_pat_) hardcoded in source" `
    -searchPath "app\src\main\java" `
    -pattern "ghp_[A-Za-z0-9]{36}|github_pat_" `
    -expectedResult "notfound" `
    -recommendation "Never hardcode GitHub PATs in source code"

# =============================================================
# LOGGING SECURITY
# =============================================================
Write-Section "LOGGING AND DATA EXPOSURE"

Check-Pattern `
    -id "LOG-001" -severity "HIGH" `
    -description "HTTP logging uses DEBUG-only BODY level (FIX-01)" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "BuildConfig.DEBUG" `
    -expectedResult "notfound" `
    -recommendation "FIX-01 must be applied: guard BODY logging with BuildConfig.DEBUG"

Check-Pattern `
    -id "LOG-002" -severity "HIGH" `
    -description "HTTP logging set to NONE in release builds" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "Level.NONE" `
    -expectedResult "notfound" `
    -recommendation "FIX-01: Add 'else Level.NONE' branch to HTTP logger"

Check-Pattern `
    -id "LOG-003" -severity "MEDIUM" `
    -description "Error bodies not leaked to caller in release (FIX-07)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\AIRepositoryImpl.kt" `
    -pattern "BuildConfig.DEBUG" `
    -expectedResult "notfound" `
    -recommendation "FIX-07: Guard errorBody() leakage with BuildConfig.DEBUG check"

# =============================================================
# NETWORK SECURITY
# =============================================================
Write-Section "NETWORK SECURITY"

Check-Pattern `
    -id "NET-001" -severity "HIGH" `
    -description "Certificate pinning configured in OkHttp (FIX-05)" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "CertificatePinner" `
    -expectedResult "notfound" `
    -recommendation "FIX-05 must be applied: add CertificatePinner to OkHttpClient"

Check-Pattern `
    -id "NET-002" -severity "HIGH" `
    -description "Network security config enforces HTTPS-only traffic" `
    -searchPath "app\src\main\res\xml\network_security_config.xml" `
    -pattern "cleartextTrafficPermitted" `
    -expectedResult "notfound" `
    -recommendation "Add network_security_config.xml with cleartextTrafficPermitted=false"

Check-Pattern `
    -id "NET-003" -severity "HIGH" `
    -description "networkSecurityConfig referenced in AndroidManifest" `
    -searchPath "app\src\main\AndroidManifest.xml" `
    -pattern "networkSecurityConfig" `
    -expectedResult "notfound" `
    -recommendation "Add android:networkSecurityConfig to <application>"

Check-Pattern `
    -id "NET-004" -severity "MEDIUM" `
    -description "OkHttp timeouts configured to prevent slow-loris attacks" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "connectTimeout|readTimeout" `
    -expectedResult "notfound" `
    -recommendation "Set connect/read/write timeouts on OkHttpClient"

# =============================================================
# DATA STORAGE SECURITY
# =============================================================
Write-Section "DATA STORAGE SECURITY"

Check-Pattern `
    -id "STG-001" -severity "HIGH" `
    -description "SQLCipher dependency present for Room DB encryption (FIX-09)" `
    -searchPath "app\build.gradle" `
    -pattern "sqlcipher" `
    -expectedResult "notfound" `
    -recommendation "FIX-09: Add net.zetetic:android-database-sqlcipher dependency"

Check-Pattern `
    -id "STG-002" -severity "HIGH" `
    -description "SupportFactory (SQLCipher) applied to Room builder (FIX-09)" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "SupportFactory" `
    -expectedResult "notfound" `
    -recommendation "FIX-09: Apply SupportFactory to Room.databaseBuilder"

Check-Pattern `
    -id "STG-003" -severity "HIGH" `
    -description "DatabaseKeyManager encrypts Room DB key (FIX-09)" `
    -searchPath "app\src\main\java\com\innogen\aipro\utils\DatabaseKeyManager.kt" `
    -pattern "EncryptedSharedPreferences" `
    -expectedResult "notfound" `
    -recommendation "FIX-09: Create DatabaseKeyManager with EncryptedSharedPreferences"

Check-Pattern `
    -id "STG-004" -severity "MEDIUM" `
    -description "Explicit Room migrations replace destructive fallback (FIX-09)" `
    -searchPath "app\src\main\java\com\innogen\aipro\di\AppModule.kt" `
    -pattern "fallbackToDestructiveMigration" `
    -expectedResult "found" `
    -recommendation "FIX-09: Replace with addMigrations(MIGRATION_1_2)"

# =============================================================
# ANDROID MANIFEST SECURITY
# =============================================================
Write-Section "ANDROID MANIFEST SECURITY"

Check-Pattern `
    -id "MAN-001" -severity "HIGH" `
    -description "allowBackup set to false (FIX-02)" `
    -searchPath "app\src\main\AndroidManifest.xml" `
    -pattern 'allowBackup="true"' `
    -expectedResult "found" `
    -recommendation "FIX-02: Set android:allowBackup=false"

Check-Pattern `
    -id "MAN-002" -severity "MEDIUM" `
    -description "FCM service set to android:exported=false" `
    -searchPath "app\src\main\AndroidManifest.xml" `
    -pattern 'android:exported="false"' `
    -expectedResult "notfound" `
    -recommendation "Add android:exported=false to FCM service element"

Check-Pattern `
    -id "MAN-003" -severity "MEDIUM" `
    -description "Sensitive data excluded from backup (FIX-02 companion)" `
    -searchPath "app\src\main\res\xml\data_extraction_rules.xml" `
    -pattern "innogen_db" `
    -expectedResult "notfound" `
    -recommendation "Add Room DB files to data_extraction_rules.xml exclusions"

# =============================================================
# BUILD CONFIGURATION SECURITY
# =============================================================
Write-Section "BUILD CONFIGURATION SECURITY"

Check-Pattern `
    -id "BLD-001" -severity "HIGH" `
    -description "ProGuard/R8 enabled in release builds (FIX-04)" `
    -searchPath "app\build.gradle" `
    -pattern "minifyEnabled true" `
    -expectedResult "notfound" `
    -recommendation "FIX-04: Set minifyEnabled=true in release buildType"

Check-Pattern `
    -id "BLD-002" -severity "HIGH" `
    -description "Resource shrinking enabled in release builds (FIX-04)" `
    -searchPath "app\build.gradle" `
    -pattern "shrinkResources true" `
    -expectedResult "notfound" `
    -recommendation "FIX-04: Set shrinkResources=true in release buildType"

Check-Pattern `
    -id "BLD-003" -severity "MEDIUM" `
    -description "Comprehensive ProGuard rules cover all libraries (FIX-04)" `
    -searchPath "app\proguard-rules.pro" `
    -pattern "sqlcipher|SQLCipher" `
    -expectedResult "found" `
    -recommendation "FIX-04: ProGuard rules are in place"

# =============================================================
# INJECTION VULNERABILITY CHECKS
# =============================================================
Write-Section "INJECTION VULNERABILITY CHECKS"

Check-Pattern `
    -id "INJ-001" -severity "HIGH" `
    -description "LLM prompt injection sanitizer implemented (FIX-07)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\AIRepositoryImpl.kt" `
    -pattern "sanitizeUserInput" `
    -expectedResult "notfound" `
    -recommendation "FIX-07: Add sanitizeUserInput() function to AIRepositoryImpl"

Check-Pattern `
    -id "INJ-002" -severity "HIGH" `
    -description "Code input sanitizer implemented (FIX-07)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\AIRepositoryImpl.kt" `
    -pattern "sanitizeCodeInput" `
    -expectedResult "notfound" `
    -recommendation "FIX-07: Add sanitizeCodeInput() function to AIRepositoryImpl"

Check-Pattern `
    -id "INJ-003" -severity "HIGH" `
    -description "Injection patterns list covers jailbreak attempts (FIX-07)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\AIRepositoryImpl.kt" `
    -pattern "jailbreak" `
    -expectedResult "notfound" `
    -recommendation "FIX-07: Add jailbreak pattern to injection filter"

# =============================================================
# RATE LIMITING
# =============================================================
Write-Section "RATE LIMITING AND ABUSE PREVENTION"

Check-Pattern `
    -id "RATE-001" -severity "MEDIUM" `
    -description "Client-side rate limiting in GenerationViewModel (FIX-11)" `
    -searchPath "app\src\main\java\com\innogen\aipro\presentation\generation\GenerationViewModel.kt" `
    -pattern "MIN_INTERVAL_MS|lastGenerationTime" `
    -expectedResult "notfound" `
    -recommendation "FIX-11: Add rate limiting to GenerationViewModel.startGeneration()"

Check-Pattern `
    -id "RATE-002" -severity "MEDIUM" `
    -description "Cooldown timer exposed to UI for user feedback (FIX-11)" `
    -searchPath "app\src\main\java\com\innogen\aipro\presentation\generation\GenerationViewModel.kt" `
    -pattern "rateLimitCooldownSec|isRateLimited" `
    -expectedResult "notfound" `
    -recommendation "FIX-11: Expose isRateLimited and rateLimitCooldownSec in UiState"

# =============================================================
# FCM SECURITY
# =============================================================
Write-Section "FCM AND PUSH NOTIFICATION SECURITY"

Check-Pattern `
    -id "FCM-001" -severity "MEDIUM" `
    -description "FCM token registered to Firestore on rotation (FIX-10)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\fcm\InnoGenMessagingService.kt" `
    -pattern "fcmToken" `
    -expectedResult "notfound" `
    -recommendation "FIX-10: Implement onNewToken to save FCM token to Firestore"

Check-Pattern `
    -id "FCM-002" -severity "MEDIUM" `
    -description "FCM uses authenticated user UID for token registration (FIX-10)" `
    -searchPath "app\src\main\java\com\innogen\aipro\data\remote\fcm\InnoGenMessagingService.kt" `
    -pattern "FirebaseAuth.getInstance" `
    -expectedResult "notfound" `
    -recommendation "FIX-10: Check auth.currentUser before registering FCM token"

# =============================================================
# FIRESTORE RULES
# =============================================================
Write-Section "FIRESTORE SECURITY RULES"

Check-Pattern `
    -id "FST-001" -severity "HIGH" `
    -description "Firestore security rules file exists (FIX-08)" `
    -searchPath "firestore.rules" `
    -pattern "deny-by-default|allow read, write: if false" `
    -expectedResult "notfound" `
    -recommendation "FIX-08: Create firestore.rules with deny-by-default policy"

Check-Pattern `
    -id "FST-002" -severity "HIGH" `
    -description "Firestore rules enforce owner-only access for projects" `
    -searchPath "firestore.rules" `
    -pattern "userId == request.auth.uid" `
    -expectedResult "notfound" `
    -recommendation "FIX-08: Firestore rules must check userId == request.auth.uid"

# =============================================================
# RESULTS SUMMARY
# =============================================================
Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "              SCAN RESULTS SUMMARY                              " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ("  Total Checks   : {0}" -f $totalChecks)  -ForegroundColor White
Write-Host ("  PASS           : {0}" -f $totalPassed)  -ForegroundColor Green
Write-Host ("  FAIL           : {0}" -f $totalFails)   -ForegroundColor Red
Write-Host "----------------------------------------------------------------" -ForegroundColor Cyan

$passRate   = if ($totalChecks -gt 0) { [math]::Round(($totalPassed / $totalChecks) * 100, 1) } else { 0 }
$scoreColor = if ($passRate -ge 90) { "Green" } elseif ($passRate -ge 75) { "Yellow" } else { "Red" }
Write-Host ("  Pass Rate      : {0}%" -f $passRate) -ForegroundColor $scoreColor
Write-Host ("  Report saved   : {0}" -f (Split-Path $reportFile -Leaf)) -ForegroundColor Gray
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

Add-Content $reportFile ""
Add-Content $reportFile "==================================================="
Add-Content $reportFile "SUMMARY: Total=$totalChecks | Passed=$totalPassed | Failed=$totalFails | Pass Rate=$passRate%"
Add-Content $reportFile "==================================================="

if ($totalFails -gt 0) {
    Write-Host "[!] $totalFails check(s) failed. Review and apply recommended fixes." -ForegroundColor Yellow
    Write-Host "    Report: $reportFile" -ForegroundColor DarkGray
    exit 1
} else {
    Write-Host "[OK] All $totalChecks security checks passed! Security score: $passRate%" -ForegroundColor Green
    exit 0
}
