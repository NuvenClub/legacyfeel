$ErrorActionPreference = 'Stop'
$plugins = Join-Path $PSScriptRoot 'quick-lab\plugins'
New-Item -ItemType Directory -Force -Path $plugins | Out-Null

$artifacts = @(
    @{ Name = 'ViaVersion.jar'; Version = '5.12.0'; Url = 'https://github.com/ViaVersion/ViaVersion/releases/download/5.12.0/ViaVersion-5.12.0.jar' },
    @{ Name = 'ViaBackwards.jar'; Version = '5.12.0'; Url = 'https://github.com/ViaVersion/ViaBackwards/releases/download/5.12.0/ViaBackwards-5.12.0.jar' },
    @{ Name = 'ViaRewind.jar'; Version = '4.2.0'; Url = 'https://github.com/ViaVersion/ViaRewind/releases/download/4.2.0/ViaRewind-4.2.0.jar' }
)

Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($artifact in $artifacts) {
    $destination = Join-Path $plugins $artifact.Name
    if (Test-Path $destination) {
        $archive = [IO.Compression.ZipFile]::OpenRead($destination)
        try {
            $metadata = $archive.GetEntry('plugin.yml')
            if (-not $metadata) { $metadata = $archive.GetEntry('paper-plugin.yml') }
            if ($metadata) {
                $reader = [IO.StreamReader]::new($metadata.Open())
                try { $contents = $reader.ReadToEnd() } finally { $reader.Dispose() }
                $versionLine = $contents -split "`n" | Where-Object { $_ -match '^version:' } | Select-Object -First 1
                $installedVersion = ($versionLine -replace '^version:\s*', '').Trim().Trim('"').Trim("'")
                if ($installedVersion -eq $artifact.Version) {
                    Write-Host "Já está atualizado: $destination ($($artifact.Version))"
                    continue
                }
            }
        } finally { $archive.Dispose() }
    }
    Invoke-WebRequest -Uri $artifact.Url -OutFile $destination
    Write-Host "Atualizado: $destination"
}
