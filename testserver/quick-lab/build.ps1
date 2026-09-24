$ErrorActionPreference = 'Stop'
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$viaUpdate = Join-Path $projectRoot 'testserver\update-viaversion.ps1'
& $viaUpdate
$javaRoot = Resolve-Path (Join-Path $projectRoot '.tools\java25\jdk-25.0.4.1+1')
$gradle = Resolve-Path (Join-Path $projectRoot '.tools\gradle\gradle-9.5.1\bin\gradle.bat')
$env:JAVA_HOME = $javaRoot.Path
& $gradle.Path -p (Join-Path $projectRoot 'plugin') clean test build --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Build do plugin falhou.' }
& (Join-Path $projectRoot 'mod\gradlew.bat') -p (Join-Path $projectRoot 'mod') clean build --console=plain --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Build do mod falhou.' }
Copy-Item -LiteralPath (Join-Path $projectRoot 'plugin\build\libs\legacyfeel-server-0.1.0.jar') -Destination (Join-Path $PSScriptRoot 'plugins\LegacyFeel-Server.jar') -Force
$ocmRoot = Join-Path $PSScriptRoot 'plugins\OldCombatMechanics'
New-Item -ItemType Directory -Path $ocmRoot -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'oldcombatmechanics-config.yml') -Destination (Join-Path $ocmRoot 'config.yml') -Force
$grimRoot = Join-Path $PSScriptRoot 'plugins\GrimAC'
New-Item -ItemType Directory -Path $grimRoot -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'grim-config.yml') -Destination (Join-Path $grimRoot 'config.yml') -Force
Write-Host "Plugin: $PSScriptRoot\plugins\LegacyFeel-Server.jar"
Write-Host "Mod: $projectRoot\mod\build\libs\legacyfeel-0.1.0.jar"
