$ErrorActionPreference = 'Stop'
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$java = Resolve-Path (Join-Path $projectRoot '.tools\java25\jdk-25.0.4.1+1\bin\java.exe')
if (-not (Test-Path (Join-Path $PSScriptRoot 'paper.jar'))) { & (Join-Path $PSScriptRoot 'install.ps1') }
& (Join-Path $PSScriptRoot 'build.ps1')
Push-Location $PSScriptRoot
try { & $java.Path '-Xms1G' '-Xmx2G' '-jar' 'paper.jar' '--nogui' } finally { Pop-Location }
