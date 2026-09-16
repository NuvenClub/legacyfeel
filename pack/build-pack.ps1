param(
    [string]$Output = (Join-Path $PSScriptRoot '..\lab-server\build\resources\skywars-lab-pack.zip')
)

$ErrorActionPreference = 'Stop'
$outputPath = [System.IO.Path]::GetFullPath($Output)
$outputDirectory = Split-Path -Parent $outputPath
New-Item -ItemType Directory -Force $outputDirectory | Out-Null

Add-Type -AssemblyName System.IO.Compression
if (Test-Path $outputPath) { Remove-Item -LiteralPath $outputPath }
$archive = [System.IO.Compression.ZipFile]::Open($outputPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $sources = @((Get-Item (Join-Path $PSScriptRoot 'pack.mcmeta')))
    $sources += Get-ChildItem (Join-Path $PSScriptRoot 'assets') -File -Recurse
    $sources += Get-ChildItem (Join-Path $PSScriptRoot 'manifest') -File -Recurse
    foreach ($source in ($sources | Sort-Object FullName)) {
        $relative = [System.IO.Path]::GetRelativePath($PSScriptRoot, $source.FullName).Replace('\', '/')
        $entry = $archive.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
        $entry.LastWriteTime = [DateTimeOffset]::new(2026, 1, 1, 0, 0, 0, [TimeSpan]::Zero)
        $inputStream = [System.IO.File]::OpenRead($source.FullName)
        $outputStream = $entry.Open()
        try { $inputStream.CopyTo($outputStream) }
        finally { $outputStream.Dispose(); $inputStream.Dispose() }
    }
} finally {
    $archive.Dispose()
}

$sha1 = (Get-FileHash -Algorithm SHA1 $outputPath).Hash.ToLowerInvariant()
Write-Output "Pack: $outputPath"
Write-Output "SHA1: $sha1"
