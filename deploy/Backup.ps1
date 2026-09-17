param(
    [string]$ProjectName = 'dazbones',
    [string]$EnvFile = "$PSScriptRoot/.env",
    [string]$OutputRoot = "$PSScriptRoot/backups",
    [ValidateRange(1,100)][int]$Keep = 5
)
$ErrorActionPreference = 'Stop'
$composeArgs = @('compose','--project-name',$ProjectName,'--env-file',$EnvFile,'-f',"$PSScriptRoot/compose.yml")
function Invoke-Compose { & docker @composeArgs @args; if ($LASTEXITCODE -ne 0) { throw 'Docker Compose command failed' } }
$root = [IO.Path]::GetFullPath($OutputRoot)
[IO.Directory]::CreateDirectory($root) | Out-Null
$folder = Join-Path $root ('backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
[IO.Directory]::CreateDirectory($folder) | Out-Null
$running = Invoke-Compose ps --status running -q app
$temp = '/tmp/dazbones-backup-' + [guid]::NewGuid().ToString('N') + '.sql'
try {
    if ($running) { Invoke-Compose stop app }
    Invoke-Compose exec -T db sh -c ('MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF --hex-blob "$MYSQL_DATABASE" > ' + $temp)
    Invoke-Compose cp "db:$temp" (Join-Path $folder 'database.sql')
    Invoke-Compose run --rm --no-deps --user 0:0 --volume "${folder}:/backup" --entrypoint tar app -czf /backup/images.tar.gz -C /data/images .
    $files = @('database.sql','images.tar.gz') | ForEach-Object {
        $file = Join-Path $folder $_
        if ((Get-Item -LiteralPath $file).Length -eq 0) { throw 'Empty backup file' }
        @{ name = $_; sha256 = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash }
    }
    @{ format=1; createdAt=(Get-Date).ToUniversalTime().ToString('o'); project=$ProjectName; files=@($files) } |
        ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $folder 'manifest.json') -Encoding utf8
    # Only prune complete, tool-created backups within this exact root, after successful backup.
    $old = Get-ChildItem -LiteralPath $root -Directory | Where-Object {
        $_.Name -match '^backup-\d{8}-\d{6}-[a-f0-9]{8}$' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'manifest.json'))
    } | Sort-Object Name -Descending | Select-Object -Skip $Keep
    foreach ($item in $old) {
        $resolved = [IO.Path]::GetFullPath($item.FullName)
        if ([IO.Path]::GetDirectoryName($resolved) -ne $root) { throw 'Backup retention path escaped root' }
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Refusing to prune a linked backup directory' }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
    Write-Output "Backup completed: $folder"
} finally {
    & docker @composeArgs exec -T db rm -f $temp
    if ($running) { Invoke-Compose start app }
}
