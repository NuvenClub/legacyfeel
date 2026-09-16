param(
    [int]$Port = 25578,
    [string]$Pack = (Join-Path $PSScriptRoot 'dist\skywars-lab-pack.zip')
)

$ErrorActionPreference = 'Stop'
$packPath = [System.IO.Path]::GetFullPath($Pack)
if (-not (Test-Path -LiteralPath $packPath)) {
    throw "Pacote não encontrado: $packPath. Execute build-pack.ps1 primeiro."
}

$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://127.0.0.1:$Port/")
$listener.Start()
Write-Host "Pacote local disponível em http://127.0.0.1:$Port/skywars-lab-pack.zip"
Write-Host "Pressione Ctrl+C para encerrar."

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        try {
            if ($context.Request.Url.AbsolutePath -ne '/skywars-lab-pack.zip') {
                $context.Response.StatusCode = 404
                $context.Response.Close()
                continue
            }
            $bytes = [System.IO.File]::ReadAllBytes($packPath)
            $context.Response.StatusCode = 200
            $context.Response.ContentType = 'application/zip'
            $context.Response.ContentLength64 = $bytes.Length
            $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
            $context.Response.OutputStream.Close()
        } catch {
            $context.Response.Abort()
        }
    }
} finally {
    $listener.Stop()
    $listener.Close()
}
