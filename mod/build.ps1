param(
    [ValidateSet('26.2', '26.3')]
    [string]$MinecraftVersion = '26.3',
    [string]$ModVersion = '0.1.1'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$javaHome = Join-Path $projectRoot '.tools\java25\jdk-25.0.4.1+1'
if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    throw "Java 25 não encontrado em $javaHome. Execute primeiro testserver/quick-lab/install.ps1."
}
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$fabricApiVersion = switch ($MinecraftVersion) {
    '26.2' { '0.160.0+26.2' }
    '26.3' { '0.161.0+26.3' }
}
$modVersion = "$ModVersion+mc$MinecraftVersion"

Push-Location $PSScriptRoot
try {
    & .\gradlew.bat clean build `
        "-Pminecraft_version=$MinecraftVersion" `
        "-Pfabric_api_version=$fabricApiVersion" `
        "-Pversion=$modVersion" `
        --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "A compilação do LegacyFeel para Minecraft $MinecraftVersion falhou."
    }
} finally {
    Pop-Location
}
