[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$ServerOnly,
    [switch]$ClientOnly
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

function Resolve-Jdk {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }
    $roots = 'C:\Program Files\Java','C:\Program Files\Eclipse Adoptium',
             'C:\Program Files\Microsoft','C:\Program Files\Zulu',
             'C:\Program Files\Amazon Corretto','C:\Program Files (x86)\Java' |
             Where-Object { Test-Path $_ }
    foreach ($r in $roots) {
        $jdk = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
               Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
               Sort-Object Name -Descending | Select-Object -First 1
        if ($jdk) { return $jdk.FullName }
    }
    throw "JDK not found. Set JAVA_HOME or install a JDK."
}

$JdkHome   = Resolve-Jdk
$Java      = Join-Path $JdkHome 'bin\java.exe'
$Javac     = Join-Path $JdkHome 'bin\javac.exe'
$Classpath = 'bin;lib/org.json.jar;lib/sqlite-jdbc.jar;lib/slf4j-api-2.0.13.jar;lib/slf4j-nop-2.0.13.jar'

if ($Rebuild -or -not (Test-Path 'bin\com\quiz\server\QuizServer.class')) {
    if (-not (Test-Path 'bin')) { New-Item -ItemType Directory -Path bin | Out-Null }
    $sources = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    & $Javac -encoding UTF-8 -d bin -cp 'lib/org.json.jar;lib/sqlite-jdbc.jar;lib/slf4j-api-2.0.13.jar;lib/slf4j-nop-2.0.13.jar' @sources
    if ($LASTEXITCODE -ne 0) { throw "javac failed ($LASTEXITCODE)" }
}

function Start-JavaWindow {
    param([string]$Title, [string]$MainClass)
    $inner = "chcp 65001 > `$null; `$Host.UI.RawUI.WindowTitle = '$Title'; Set-Location '$ProjectRoot'; & '$Java' -cp '$Classpath' $MainClass"
    Start-Process powershell -ArgumentList '-NoExit', '-Command', $inner
}

if (-not $ClientOnly) {
    Start-JavaWindow -Title 'QuizServer' -MainClass 'com.quiz.server.QuizServer'
    Start-Sleep -Seconds 1
}
if (-not $ServerOnly) {
    Start-JavaWindow -Title 'QuizClient' -MainClass 'com.quiz.client.LoginScreen'
}
