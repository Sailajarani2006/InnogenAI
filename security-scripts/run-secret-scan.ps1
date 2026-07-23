# ============================================================
#  run-secret-scan.ps1  —  Secret & Credential Scanner
#  InnoGen AI Pro  |  Uses Gitleaks + custom regex patterns
# ============================================================

$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDir   = Join-Path $projectRoot "Vulnerability Test Results"
$timestamp   = Get-Date -Format "yyyyMMdd_HHmmss"
$reportJson  = Join-Path $reportDir "gitleaks-report-$timestamp.json"
$reportTxt   = Join-Path $reportDir "secret-scan-$timestamp.txt"

New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

Write-Host ""
Write-Host "🔑 Running Secret & Credential Scan..." -ForegroundColor Cyan
Write-Host ""

$leaksFound = 0

# ── Gitleaks ────────────────────────────────────────────────
if (Get-Command "gitleaks" -ErrorAction SilentlyContinue) {
    Write-Host "  [1/2] Gitleaks — full git history scan..." -ForegroundColor Yellow
    gitleaks detect `
        --source $projectRoot `
        --report-path $reportJson `
        --report-format json `
        --no-banner `
        2>&1 | Out-Null

    if (Test-Path $reportJson) {
        $leaks = Get-Content $reportJson | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($leaks -and $leaks.Count -gt 0) {
            $leaksFound = $leaks.Count
            Write-Host "  ❌ Gitleaks found $leaksFound secret(s) in git history!" -ForegroundColor Red
            $leaks | ForEach-Object {
                Write-Host ("     → [{0}] {1} in {2}:{3}" -f $_.RuleID, $_.Description, $_.File, $_.StartLine) -ForegroundColor DarkYellow
            }
        } else {
            Write-Host "  ✅ Gitleaks — No secrets in git history" -ForegroundColor Green
        }
    }
} else {
    Write-Host "  ⚠️  Gitleaks not installed — skipping git history scan" -ForegroundColor DarkYellow
    Write-Host "     Install: choco install gitleaks  OR  scoop install gitleaks" -ForegroundColor DarkGray
}

# ── Custom Pattern Scan ───────────────────────────────────────
Write-Host "  [2/2] Custom regex — scanning for hardcoded credentials..." -ForegroundColor Yellow

$patterns = @(
    @{ Pattern = 'gsk_[a-zA-Z0-9]{40,}';           Label = "Groq API Key" },
    @{ Pattern = 'ghp_[a-zA-Z0-9]{36}';             Label = "GitHub PAT (classic)" },
    @{ Pattern = 'github_pat_[a-zA-Z0-9_]{82}';     Label = "GitHub PAT (fine-grained)" },
    @{ Pattern = 'ya29\.[0-9A-Za-z\-_]+';           Label = "Google OAuth2 Token" },
    @{ Pattern = 'AIza[0-9A-Za-z\-_]{35}';          Label = "Google API Key" },
    @{ Pattern = 'AAAA[A-Za-z0-9_-]{7}:[A-Za-z0-9_-]{140}'; Label = "Firebase Cloud Messaging Key" },
    @{ Pattern = '[0-9]+-[0-9A-Za-z_]{32}\.apps\.googleusercontent\.com'; Label = "Google OAuth Client ID" },
    @{ Pattern = 'password\s*=\s*["\x27][^"\']{4,}'; Label = "Hardcoded Password" },
    @{ Pattern = 'secret\s*=\s*["\x27][^"\']{4,}';  Label = "Hardcoded Secret" }
)

$scanPaths = @(
    (Join-Path $projectRoot "app\src"),
    (Join-Path $projectRoot "app\build.gradle"),
    (Join-Path $projectRoot "local.properties"),
    (Join-Path $projectRoot "gradle.properties")
)

$customFindings = @()
foreach ($p in $patterns) {
    foreach ($scanPath in $scanPaths) {
        if (Test-Path $scanPath) {
            $hits = Get-ChildItem $scanPath -Recurse -File -ErrorAction SilentlyContinue |
                    Where-Object { $_.Extension -in @('.kt','.java','.gradle','.properties','.xml','.json','.yml') } |
                    Select-String -Pattern $p.Pattern -ErrorAction SilentlyContinue
            if ($hits) {
                foreach ($h in $hits) {
                    $customFindings += "  ❌ [$($p.Label)] found in $($h.Filename):$($h.LineNumber)"
                    $leaksFound++
                }
            }
        }
    }
}

if ($customFindings.Count -gt 0) {
    Write-Host ""
    Write-Host "  Custom pattern scan findings:" -ForegroundColor Red
    $customFindings | ForEach-Object { Write-Host $_ -ForegroundColor DarkYellow }
} else {
    Write-Host "  ✅ Custom pattern scan — No obvious hardcoded secrets found in source files" -ForegroundColor Green
    Write-Host "     Note: BuildConfig embeds secrets at compile time (check APK for OPENAI_API_KEY)" -ForegroundColor DarkGray
}

# ── local.properties warning ─────────────────────────────────
Write-Host ""
Write-Host "  [CHECK] local.properties in .gitignore..." -ForegroundColor Yellow
$gitignore = Join-Path $projectRoot ".gitignore"
if (Test-Path $gitignore) {
    $ignored = Select-String -Path $gitignore -Pattern "local.properties" -ErrorAction SilentlyContinue
    if ($ignored) {
        Write-Host "  ✅ local.properties is in .gitignore" -ForegroundColor Green
    } else {
        Write-Host "  ❌ local.properties NOT in .gitignore — API keys may be committed to git!" -ForegroundColor Red
        $leaksFound++
    }
} else {
    Write-Host "  ⚠️  No .gitignore found" -ForegroundColor DarkYellow
}

# ── Summary ──────────────────────────────────────────────────
Write-Host ""
if ($leaksFound -eq 0) {
    Write-Host "✅ Secret scan complete — No leaks detected" -ForegroundColor Green
} else {
    Write-Host "❌ Secret scan complete — $leaksFound potential secret(s) found!" -ForegroundColor Red
    Write-Host "   Report: $reportJson" -ForegroundColor DarkGray
    exit 1
}
