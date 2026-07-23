# ============================================================
#  run-sast.ps1  —  SAST Scan Runner (Semgrep)
#  InnoGen AI Pro Security Assessment
# ============================================================

$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDir   = Join-Path $projectRoot "Vulnerability Test Results"
$timestamp   = Get-Date -Format "yyyyMMdd_HHmmss"

Write-Host ""
Write-Host "🔬 Running SAST Analysis with Semgrep..." -ForegroundColor Cyan
Write-Host "   Target: $projectRoot" -ForegroundColor DarkGray
Write-Host ""

# Check Semgrep is installed
if (-not (Get-Command "semgrep" -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Semgrep not installed. Run: pip install semgrep" -ForegroundColor Red
    exit 1
}

$semgrepVer = semgrep --version 2>$null
Write-Host "  Semgrep: $semgrepVer" -ForegroundColor Gray

# Output files
$jsonReport  = Join-Path $reportDir "semgrep-report-$timestamp.json"
$sarifReport = Join-Path $reportDir "semgrep-report-$timestamp.sarif"
$textReport  = Join-Path $reportDir "semgrep-report-$timestamp.txt"

New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

# ── Run Full Scan ─────────────────────────────────────────────
Write-Host "  [1/4] Running Android rules..." -ForegroundColor Yellow
semgrep scan --config=p/android --quiet --output $textReport . 2>&1 | Out-Null

Write-Host "  [2/4] Running Kotlin rules..." -ForegroundColor Yellow
semgrep scan --config=p/kotlin --quiet . 2>&1 | Out-Null

Write-Host "  [3/4] Running Secrets detection..." -ForegroundColor Yellow
semgrep scan --config=p/secrets --quiet . 2>&1 | Out-Null

Write-Host "  [4/4] Running OWASP Top 10 rules (JSON output)..." -ForegroundColor Yellow
semgrep scan `
    --config=p/android `
    --config=p/kotlin `
    --config=p/secrets `
    --config=p/owasp-top-ten `
    --json `
    --output $jsonReport `
    . 2>&1 | Out-Null

# ── SARIF for GitHub ─────────────────────────────────────────
Write-Host "  [4/4] Generating SARIF report for GitHub upload..." -ForegroundColor Yellow
semgrep scan `
    --config=p/secrets `
    --sarif `
    --output $sarifReport `
    . 2>&1 | Out-Null

Write-Host ""
Write-Host "✅ SAST scan complete!" -ForegroundColor Green
Write-Host "   JSON report  : $jsonReport" -ForegroundColor DarkGray
Write-Host "   SARIF report : $sarifReport" -ForegroundColor DarkGray
Write-Host "   Text report  : $textReport" -ForegroundColor DarkGray
Write-Host ""
Write-Host "💡 Tip: Upload the SARIF file to GitHub Security → Code Scanning" -ForegroundColor DarkCyan
