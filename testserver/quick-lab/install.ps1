$ErrorActionPreference = 'Stop'
$labRoot = $PSScriptRoot
$pluginRoot = Join-Path $labRoot 'plugins'
New-Item -ItemType Directory -Path $pluginRoot -Force | Out-Null

function Save-VerifiedFile([string]$Url, [string]$Target, [string]$Sha512) {
    Invoke-WebRequest -Uri $Url -OutFile $Target
    $actual = (Get-FileHash -LiteralPath $Target -Algorithm SHA512).Hash.ToLowerInvariant()
    if ($actual -ne $Sha512.ToLowerInvariant()) { throw "Checksum divergente: $Target" }
}

$paper = Invoke-RestMethod 'https://fill.papermc.io/v3/projects/paper/versions/26.2/builds'
$stable = $paper | Where-Object channel -eq 'STABLE' | Sort-Object id -Descending | Select-Object -First 1
$download = $stable.downloads.'server:default'
Invoke-WebRequest -Uri $download.url -OutFile (Join-Path $labRoot 'paper.jar')
$actualPaper = (Get-FileHash -LiteralPath (Join-Path $labRoot 'paper.jar') -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualPaper -ne $download.checksums.sha256.ToLowerInvariant()) { throw 'Checksum divergente: paper.jar' }

$artifacts = @(
    @('ViaVersion.jar', 'https://cdn.modrinth.com/data/P1OZGk5p/versions/I7EtZ7sn/ViaVersion-5.12.0-SNAPSHOT.jar', 'f12b12c82085c671f67cc41987e1b40e8e177a4d1806388ad05c121571ff61b83716f9d59a98095b2ca4e5e716d3856e2db14113d1a7a3b2533486764627165f'),
    @('ViaBackwards.jar', 'https://cdn.modrinth.com/data/NpvuJQoq/versions/GB5LJQxR/ViaBackwards-5.12.0-SNAPSHOT.jar', '07a30199ce6784fa48fe2809c24d4bfa11db91ab325764dad6545e69b487867665d3c09277e6fea27485e4f3a199e2b866d01ae7363f19667c5596a38536ea3c'),
    @('ViaRewind.jar', 'https://cdn.modrinth.com/data/TbHIxhx5/versions/FkS8Q0YI/ViaRewind-4.1.4-SNAPSHOT.jar', 'f2d3e4aa0dff2d6283de68aa3540b178fe2cb924d9e6fe6fd9ab2b8f06f784b65a86b9fa38e971b344ba6717aa2ab7926c23b03bcd4316ba27f398080c302697'),
    @('PacketEvents.jar', 'https://cdn.modrinth.com/data/HYKaKraK/versions/h0ncTpUP/packetevents-spigot-2.13.0.jar', 'f0f85e601855a5849418df807e116a369bfb70aa8b4c25b8904bdd04cf6a483ef5d2679a345e9d690dd2221f9daff6a25be17d8b3e63cf665e12a31b0124dd27'),
    @('GrimAC.jar', 'https://cdn.modrinth.com/data/LJNGWSvH/versions/Gd6BG1HA/grimac-bukkit-2.3.74-8eb5f28.jar', 'b210eb49bce1cd4b3e0fe375193522694faa379a347244a0770f62c87ab23c92e06ce4920ee192cbe0112fa1e071191a7d642032777c1db2925e281fee175168')
)
foreach ($artifact in $artifacts) {
    Save-VerifiedFile $artifact[1] (Join-Path $pluginRoot $artifact[0]) $artifact[2]
}
Write-Host 'Quick lab instalado com downloads e checksums verificados.'
