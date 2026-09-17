param(
    [Parameter(Mandatory)][string]$BackupDirectory,
    [string]$ProjectName = 'dazbones',
    [string]$EnvFile = "$PSScriptRoot/.env",
    [switch]$ConfirmReplace
)
$ErrorActionPreference = 'Stop'
if (!$ConfirmReplace) { throw 'Restore replaces the destination DB and images. Back up first, then explicitly pass -ConfirmReplace.' }
$folder = (Resolve-Path -LiteralPath $BackupDirectory).Path
$manifest = Get-Content -LiteralPath (Join-Path $folder 'manifest.json') -Raw | ConvertFrom-Json
if ($manifest.format -ne 1 -or $manifest.files.Count -ne 2) { throw 'Unsupported backup manifest' }
foreach ($name in @('database.sql','images.tar.gz')) {
    $entry = @($manifest.files | Where-Object name -eq $name)
    if ($entry.Count -ne 1 -or (Get-FileHash -LiteralPath (Join-Path $folder $name) -Algorithm SHA256).Hash -ne $entry[0].sha256) { throw "Backup checksum mismatch: $name" }
}
$composeArgs = @('compose','--project-name',$ProjectName,'--env-file',$EnvFile,'-f',"$PSScriptRoot/compose.yml")
function Invoke-Compose { & docker @composeArgs @args; if ($LASTEXITCODE -ne 0) { throw 'Docker Compose command failed' } }
Invoke-Compose stop app
Invoke-Compose up -d --wait db
$temp = '/tmp/dazbones-restore-' + [guid]::NewGuid().ToString('N') + '.sql'
try {
    $entries = Invoke-Compose run --rm --no-deps --volume "${folder}:/backup:ro" --entrypoint tar app -tzf /backup/images.tar.gz
    if ($entries | Where-Object { $_ -match '^/|(^|/)\.\.(/|$)' }) { throw 'Unsafe image archive path' }
    Invoke-Compose cp (Join-Path $folder 'database.sql') "db:$temp"
    Invoke-Compose exec -T db sh -c ('MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < ' + $temp)
    Invoke-Compose run --rm --no-deps --user 0:0 --volume "${folder}:/backup:ro" --entrypoint sh app -c 'find /data/images -mindepth 1 -maxdepth 1 -exec rm -rf -- {} + && tar -xzf /backup/images.tar.gz -C /data/images && chown -R 10001:10001 /data/images'
    Invoke-Compose up -d app
    Write-Output 'Restore completed. Verify /health/readiness and the site before reopening access.'
} finally {
    & docker @composeArgs exec -T db rm -f $temp
    # On failure the app remains stopped so incomplete restored data is not served.
}
