# ============================================================
#  install-tools.ps1  —  One-time Security Tool Installer
#  InnoGen AI Pro Security Assessment
# ============================================================

param(
    [switch]$UseChocolatey,
    [switch]$UseScoop,
    [switch]$SkipPython
)

$ErrorActionPreference = "Continue"

function Write-Banner {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  🔒 InnoGen AI Pro — Security Tool Installer" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Step($step, $msg) {
    Write-Host "[$step] $msg" -ForegroundColor Yellow
}

function Write-OK($msg) {
    Write-Host "  ✅ $msg" -ForegroundColor Green
}

function Write-Warn($msg) {
    Write-Host "  ⚠️  $msg" -ForegroundColor DarkYellow
}

function Write-Err($msg) {
    Write-Host "  ❌ $msg" -ForegroundColor Red
}

function Test-Command($cmd) {
    return [bool](Get-Command $cmd -ErrorAction SilentlyContinue)
}

Write-Banner

# ── Node.js ──────────────────────────────────────────────────
Write-Step "1/6" "Checking Node.js..."
if (Test-Command "node") {
    $nodeVer = node --version
    Write-OK "Node.js $nodeVer already installed"
} else {
    Write-Warn "Node.js not found. Please install from https://nodejs.org"
}

# ── Python ───────────────────────────────────────────────────
Write-Step "2/6" "Checking Python..."
if (Test-Command "python") {
    $pyVer = python --version
    Write-OK "$pyVer already installed"
} else {
    Write-Warn "Python not found. Please install from https://python.org"
}

# ── Semgrep ──────────────────────────────────────────────────
Write-Step "3/6" "Installing Semgrep (SAST)..."
if (Test-Command "semgrep") {
    $semgrepVer = semgrep --version 2>$null
    Write-OK "Semgrep $semgrepVer already installed"
} else {
    if (-not $SkipPython) {
        Write-Host "  Installing via pip..." -ForegroundColor Gray
        pip install semgrep --quiet
        if (Test-Command "semgrep") {
            Write-OK "Semgrep installed successfully"
        } else {
            Write-Err "Semgrep install failed. Try: pip install semgrep"
        }
    } else {
        Write-Warn "Skipping Semgrep (SkipPython flag set)"
    }
}

# ── Gitleaks ─────────────────────────────────────────────────
Write-Step "4/6" "Installing Gitleaks (Secret Detection)..."
if (Test-Command "gitleaks") {
    $glVer = gitleaks version 2>$null
    Write-OK "Gitleaks $glVer already installed"
} else {
    $glUrl = "https://github.com/gitleaks/gitleaks/releases/latest/download/gitleaks_8.21.2_windows_x64.zip"
    $glZip = "$env:TEMP\gitleaks.zip"
    $glDest = "C:\Windows\System32"

    Write-Host "  Downloading Gitleaks binary..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri $glUrl -OutFile $glZip -UseBasicParsing
        Expand-Archive -Path $glZip -DestinationPath "$env:TEMP\gitleaks_extract" -Force
        Copy-Item "$env:TEMP\gitleaks_extract\gitleaks.exe" "$glDest\gitleaks.exe" -Force
        Remove-Item $glZip, "$env:TEMP\gitleaks_extract" -Recurse -Force -ErrorAction SilentlyContinue
        Write-OK "Gitleaks installed to $glDest"
    } catch {
        Write-Warn "Auto-install failed. Download from: https://github.com/gitleaks/gitleaks/releases"
        Write-Warn "Alternatively: choco install gitleaks  OR  scoop install gitleaks"
    }
}

# ── Trivy ────────────────────────────────────────────────────
Write-Step "5/6" "Installing Trivy (Dependency Scanner)..."
if (Test-Command "trivy") {
    $trivyVer = trivy --version 2>$null | Select-String "Version"
    Write-OK "Trivy $trivyVer already installed"
} else {
    $trivyUrl = "https://github.com/aquasecurity/trivy/releases/latest/download/trivy_0.58.2_windows-64bit.zip"
    $trivyZip = "$env:TEMP\trivy.zip"
    $trivyDest = "C:\Windows\System32"

    Write-Host "  Downloading Trivy binary..." -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri $trivyUrl -OutFile $trivyZip -UseBasicParsing
        Expand-Archive -Path $trivyZip -DestinationPath "$env:TEMP\trivy_extract" -Force
        Copy-Item "$env:TEMP\trivy_extract\trivy.exe" "$trivyDest\trivy.exe" -Force
        Remove-Item $trivyZip, "$env:TEMP\trivy_extract" -Recurse -Force -ErrorAction SilentlyContinue
        Write-OK "Trivy installed to $trivyDest"
    } catch {
        Write-Warn "Auto-install failed. Download from: https://github.com/aquasecurity/trivy/releases"
        Write-Warn "Alternatively: choco install trivy"
    }
}

# ── Excel Report Dependencies ─────────────────────────────────
Write-Step "6/6" "Installing Excel report generator dependencies..."
$reportDir = Join-Path $PSScriptRoot "..\Vulnerability Test Results"
if (Test-Path $reportDir) {
    Push-Location $reportDir
    npm install --quiet 2>$null
    Write-OK "Excel reporter dependencies installed"
    Pop-Location
} else {
    Write-Warn "Report directory not found. Run from project root."
}

# ── Summary ──────────────────────────────────────────────────
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ✅ Tool installation complete!" -ForegroundColor Green
Write-Host "  Run .\security-scripts\run-all.ps1 to start scanning" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
