# ============================================================
#  run-dependency-scan.ps1  —  Dependency Vulnerability Scanner
#  InnoGen AI Pro  |  Uses Trivy + manual version checks
# ============================================================

$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDir   = Join-Path $projectRoot "Vulnerability Test Results"
$timestamp   = Get-Date -Format "yyyyMMdd_HHmmss"
$trivyJson   = Join-Path $reportDir "trivy-report-$timestamp.json"
$trivySarif  = Join-Path $reportDir "trivy-report-$timestamp.sarif"

New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

Write-Host ""
Write-Host "📦 Running Dependency Vulnerability Scan..." -ForegroundColor Cyan
Write-Host ""

# ── Trivy ─────────────────────────────────────────────────────
if (Get-Command "trivy" -ErrorAction SilentlyContinue) {
    $trivyVer = (trivy --version 2>$null) -join ","
    Write-Host "  Trivy: $trivyVer" -ForegroundColor Gray
    Write-Host "  [1/2] Trivy filesystem scan (JSON)..." -ForegroundColor Yellow

    trivy fs $projectRoot `
        --severity CRITICAL,HIGH,MEDIUM `
        --format json `
        --output $trivyJson `
        --quiet 2>&1 | Out-Null

    trivy fs $projectRoot `
        --severity CRITICAL,HIGH,MEDIUM `
        --format sarif `
        --output $trivySarif `
        --quiet 2>&1 | Out-Null

    Write-Host "  ✅ Trivy scan complete: $trivyJson" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Trivy not installed — skipping automated CVE scan" -ForegroundColor DarkYellow
    Write-Host "     Install: choco install trivy  OR download from github.com/aquasecurity/trivy/releases" -ForegroundColor DarkGray
}

# ── Manual Android Dependency Version Check ───────────────────
Write-Host "  [2/2] Checking Android dependency versions..." -ForegroundColor Yellow
Write-Host ""

$buildGradle = Join-Path $projectRoot "app\build.gradle"
if (-not (Test-Path $buildGradle)) {
    Write-Host "  ❌ app/build.gradle not found" -ForegroundColor Red
    exit 1
}

$gradleContent = Get-Content $buildGradle -Raw

# Known outdated versions mapping: [pattern, current_detected, latest, severity, note]
$checks = @(
    @("firebase-bom:32",        "32.5.0",  "33.8.0",  "MEDIUM",  "Multiple Firebase security improvements in BOM 33.x"),
    @("retrofit2:retrofit:2.9", "2.9.0",   "2.11.0",  "LOW",     "Security improvements and OkHttp5 support"),
    @("hilt-android:2.48",      "2.48",    "2.52",    "LOW",     "Minor security and Compose improvements"),
    @("play-services-auth:20",  "20.7.0",  "21.3.0",  "MEDIUM",  "Deprecated — migrate to Credential Manager API"),
    @("accompanist-pager:0.32", "0.32.0",  "DEPRECATED","MEDIUM","No more security patches — migrate to Foundation Pager"),
    @("lottie-compose:6.1",     "6.1.0",   "6.6.0",   "LOW",     "JSON parsing security improvements"),
    @("kotlinx-coroutines-android:1.7","1.7.3","1.9.0","LOW",    "Stability and security improvements"),
    @("navigation-compose:2.7", "2.7.4",   "2.8.9",   "LOW",     "Deep link security improvements"),
    @("datastore-preferences:1.0","1.0.0", "1.1.1",   "LOW",     "Stability and concurrent access fixes"),
    @("compose-bom:2023",       "2023.10.01","2024.12.01","LOW",  "Over a year behind — many improvements")
)

$outdatedCount = 0
foreach ($check in $checks) {
    $pattern = $check[0]
    $current = $check[1]
    $latest  = $check[2]
    $sev     = $check[3]
    $note    = $check[4]

    if ($gradleContent -match [regex]::Escape($pattern)) {
        $icon  = if ($latest -eq "DEPRECATED") { "❌" } else { "⚠️ " }
        $color = switch($sev) { "MEDIUM" { "Yellow" } "LOW" { "DarkYellow" } default { "White" } }
        Write-Host ("  $icon [{0}] {1} → {2} | {3}" -f $sev, $current, $latest, $note) -ForegroundColor $color
        $outdatedCount++
    }
}

# ── Missing Security Libraries Check ─────────────────────────
Write-Host ""
Write-Host "  Checking for security library additions..." -ForegroundColor Yellow

$secLibChecks = @(
    @("security-crypto",       "androidx.security:security-crypto",   "Required for EncryptedDataStore/EncryptedSharedPreferences"),
    @("android-database-sqlcipher", "net.zetetic:android-database-sqlcipher", "Required for Room DB encryption"),
    @("play:integrity",        "com.google.android.play:integrity",   "Required for Play Integrity API checks")
)

foreach ($lib in $secLibChecks) {
    $pattern = $lib[0]
    $fullDep = $lib[1]
    $reason  = $lib[2]
    if ($gradleContent -notmatch [regex]::Escape($pattern)) {
        Write-Host ("  ⬜ MISSING: {0}" -f $fullDep) -ForegroundColor DarkCyan
        Write-Host ("     Reason: {0}" -f $reason) -ForegroundColor DarkGray
    } else {
        Write-Host ("  ✅ PRESENT: {0}" -f $fullDep) -ForegroundColor Green
    }
}

# ── Summary ──────────────────────────────────────────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         DEPENDENCY SCAN SUMMARY                  ║" -ForegroundColor Cyan
Write-Host ("║  Outdated / Deprecated : {0,-25}║" -f $outdatedCount) -ForegroundColor $(if($outdatedCount -gt 5){"Yellow"}else{"White"})
Write-Host ("║  Trivy JSON report     : {0,-25}║" -f (Split-Path $trivyJson -Leaf)) -ForegroundColor Gray
Write-Host "╚══════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 See dependency-report.md for full details and upgrade commands" -ForegroundColor DarkCyan
