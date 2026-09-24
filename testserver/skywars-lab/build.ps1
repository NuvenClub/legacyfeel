$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$viaUpdate = Join-Path $PSScriptRoot '..\update-viaversion.ps1'
& $viaUpdate
$javaHome = (Resolve-Path (Join-Path $root '.tools\java25\jdk-25.0.4.1+1')).Path
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

& (Join-Path $root 'pack\build-pack.ps1') -Output (Join-Path $root 'pack\dist\skywars-lab-pack.zip')
& (Join-Path $root 'plugin\gradlew.bat') -p (Join-Path $root 'plugin') clean test build --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Build do LegacyFeel-Server falhou.' }
& (Join-Path $root 'plugin\gradlew.bat') -p (Join-Path $root 'lab-server') clean test build --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Build do SkyWars Laboratório falhou.' }

New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot 'plugins') | Out-Null
Copy-Item (Join-Path $root 'lab-server\build\libs\skywars-lab-server-0.1.0.jar') `
    (Join-Path $PSScriptRoot 'plugins\SkyWarsLab.jar') -Force
Copy-Item (Join-Path $root 'plugin\build\libs\legacyfeel-server-0.1.0.jar') `
    (Join-Path $PSScriptRoot 'plugins\LegacyFeel-Server.jar') -Force
Copy-Item (Join-Path $root 'testserver\quick-lab\plugins\PacketEvents.jar') `
    (Join-Path $PSScriptRoot 'plugins\PacketEvents.jar') -Force
foreach ($plugin in @('ViaVersion.jar', 'ViaBackwards.jar', 'ViaRewind.jar')) {
    Copy-Item (Join-Path $root "testserver\quick-lab\plugins\$plugin") `
        (Join-Path $PSScriptRoot "plugins\$plugin") -Force
}
Copy-Item (Join-Path $root 'testserver\quick-lab\plugins\OldCombatMechanics.jar') `
    (Join-Path $PSScriptRoot 'plugins\OldCombatMechanics.jar') -Force
New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot 'plugins\LegacyFeel-Server') | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'legacyfeel-config.yml') `
    (Join-Path $PSScriptRoot 'plugins\LegacyFeel-Server\config.yml') -Force
New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot 'plugins\OldCombatMechanics') | Out-Null
Copy-Item (Join-Path $root 'testserver\quick-lab\oldcombatmechanics-config.yml') `
    (Join-Path $PSScriptRoot 'plugins\OldCombatMechanics\config.yml') -Force
Copy-Item (Join-Path $root 'testserver\quick-lab\paper.jar') (Join-Path $PSScriptRoot 'paper.jar') -Force

Write-Host 'Build pronto. Plugin, Paper e pacote local foram preparados.'
