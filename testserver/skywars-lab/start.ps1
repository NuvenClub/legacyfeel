$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$javaHome = (Resolve-Path (Join-Path $root '.tools\java25\jdk-25.0.4.1+1')).Path
$packServer = Start-Process powershell -ArgumentList @(
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
    (Join-Path $root 'pack\serve-pack.ps1')
) -WindowStyle Hidden -PassThru

try {
    Push-Location $PSScriptRoot
    & (Join-Path $javaHome 'bin\java.exe') -Xms1G -Xmx2G -jar paper.jar --nogui
} finally {
    Pop-Location
    if ($packServer -and -not $packServer.HasExited) {
        Stop-Process -Id $packServer.Id -Force
    }
}
