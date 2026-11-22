# UrbanTasker PowerShell Direct Launcher
# Runs the full-stack Spring Boot platform with SQLite

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot
if (-not $projectRoot) {
    $projectRoot = (Get-Location).Path
}

Write-Host "==========================================================" -ForegroundColor Magenta
Write-Host " Starting UrbanTasker Platform (Spring Boot + SQLite) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Magenta

# 1. Check if global 'mvn' is installed and working
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvnCmd) {
    Write-Host "[UrbanTasker] Using installed Maven..." -ForegroundColor Green
    & mvn spring-boot:run
    exit $LASTEXITCODE
}

# 2. Check if portable Maven is already extracted
$portableMvn = Join-Path $projectRoot ".mvn\maven\apache-maven-3.9.6\bin\mvn.cmd"
if (Test-Path $portableMvn) {
    Write-Host "[UrbanTasker] Using portable Maven..." -ForegroundColor Green
    & $portableMvn spring-boot:run
    exit $LASTEXITCODE
}

# 3. Use Maven Wrapper Launcher with correct classpath
$wrapperDir = Join-Path $projectRoot ".mvn\wrapper"
$wrapperJar = Join-Path $wrapperDir "maven-wrapper.jar"
$wrapperUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if (-not (Test-Path $wrapperJar)) {
    Write-Host "[UrbanTasker] Downloading maven-wrapper.jar..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar
}

Write-Host "[UrbanTasker] Launching Maven Wrapper..." -ForegroundColor Green
try {
    & java "-Dmaven.multiModuleProjectDirectory=$projectRoot" -cp $wrapperJar org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
} catch {
    Write-Host "[UrbanTasker] Wrapper encountered an issue. Setting up standalone Maven..." -ForegroundColor Yellow
    $mavenZipUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
    $mavenZipFile = Join-Path $projectRoot ".mvn\maven.zip"
    $mavenExtractDir = Join-Path $projectRoot ".mvn\maven"
    
    New-Item -ItemType Directory -Path $mavenExtractDir -Force | Out-Null
    Write-Host "[UrbanTasker] Downloading Apache Maven (one-time setup)..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $mavenZipUrl -OutFile $mavenZipFile
    Expand-Archive -Path $mavenZipFile -DestinationPath $mavenExtractDir -Force
    Remove-Item -Path $mavenZipFile -Force
    
    & $portableMvn spring-boot:run
}
