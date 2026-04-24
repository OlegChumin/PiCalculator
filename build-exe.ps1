$ErrorActionPreference = "Stop"

$projectName = "PiCalculator"
$projectVersion = "0.0.1-SNAPSHOT"
$jarName = "$projectName-$projectVersion.jar"
$buildDir = Join-Path $PSScriptRoot "build"
$inputDir = Join-Path $buildDir "exe-input"
$distDir = Join-Path $PSScriptRoot "dist"
$imageDir = Join-Path $distDir $projectName
$launcherPublishDir = Join-Path $buildDir "windows-launcher-publish"
$launcherTarget = Join-Path $distDir "$projectName-Start.exe"
$installerTarget = Join-Path $distDir "$projectName-1.0.0.exe"
$wixBin = "C:\Program Files (x86)\WiX Toolset v3.14\bin"

if (Test-Path $inputDir) {
    Remove-Item $inputDir -Recurse -Force
}

if (Test-Path $imageDir) {
    Remove-Item $imageDir -Recurse -Force
}

if (Test-Path $launcherPublishDir) {
    Remove-Item $launcherPublishDir -Recurse -Force
}

if (Test-Path $launcherTarget) {
    Remove-Item $launcherTarget -Force
}

if (Test-Path $installerTarget) {
    Remove-Item $installerTarget -Force
}

New-Item -ItemType Directory -Path $inputDir | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

if (Test-Path $wixBin) {
    $env:PATH = "$wixBin;$env:PATH"
}

& .\gradlew.bat bootJar

Copy-Item (Join-Path $buildDir "libs\$jarName") $inputDir

jpackage `
  --type app-image `
  --name $projectName `
  --input $inputDir `
  --main-jar $jarName `
  --dest $distDir `
  --java-options "-Dpi.browser.auto-open=true" `
  --java-options "-Dpi.browser.auto-open-url=http://localhost:8181/" `
  --java-options "-Dserver.port=8181" `
  --vendor "Oleg Chumin" `
  --description "Pi calculator with Spring Boot and browser UI" `
  --app-version "1.0.0"

jpackage `
  --type exe `
  --name $projectName `
  --input $inputDir `
  --main-jar $jarName `
  --dest $distDir `
  --java-options "-Dpi.browser.auto-open=true" `
  --java-options "-Dpi.browser.auto-open-url=http://localhost:8181/" `
  --java-options "-Dserver.port=8181" `
  --vendor "Oleg Chumin" `
  --description "Pi calculator with Spring Boot and browser UI" `
  --app-version "1.0.0" `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut `
  --win-per-user-install

dotnet publish .\tools\WindowsLauncher\WindowsLauncher.csproj `
  -c Release `
  -r win-x64 `
  --self-contained true `
  -p:PublishSingleFile=true `
  -p:IncludeNativeLibrariesForSelfExtract=true `
  -o $launcherPublishDir

Copy-Item (Join-Path $launcherPublishDir "WindowsLauncher.exe") $launcherTarget

Write-Output "Portable app image created: $imageDir\\$projectName.exe"
Write-Output "Windows launcher created: $launcherTarget"
Write-Output "Windows installer created: $installerTarget"
