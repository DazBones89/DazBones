param([string]$OutputDirectory="$PSScriptRoot/../build/security-scan")
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($OutputDirectory)
[IO.Directory]::CreateDirectory($root) | Out-Null
$scanner='aquasec/trivy@sha256:62b1e65e8869bc4b4c6aa4fa2b21595256c7c2f6018a9d9ad61caf87187c1969'
$summary=@()
foreach($name in @('site','proxy','mysql')) {
    $image="dazbones-${name}:local"
    & docker save -o (Join-Path $root "$name.tar") $image
    if($LASTEXITCODE -ne 0){throw "Cannot export $image"}
    & docker run --rm -v "${root}:/scan" -v "${root}/cache:/root/.cache/trivy" $scanner image --quiet --input "/scan/$name.tar" --scanners vuln --format json --output "/scan/$name.json"
    if($LASTEXITCODE -ne 0){throw "Scan failed for $image"}
    $report=Get-Content -LiteralPath (Join-Path $root "$name.json") -Raw | ConvertFrom-Json
    $findings=@($report.Results.Vulnerabilities | Where-Object {$_})
    $summary += [pscustomobject]@{
        image=$image; imageId=$report.Metadata.ImageID
        critical=@($findings | Where-Object Severity -eq 'CRITICAL').Count
        high=@($findings | Where-Object Severity -eq 'HIGH').Count
        medium=@($findings | Where-Object Severity -eq 'MEDIUM').Count
        low=@($findings | Where-Object Severity -eq 'LOW').Count
        unknown=@($findings | Where-Object Severity -eq 'UNKNOWN').Count
    }
}
$summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $root 'summary.json') -Encoding utf8
$summary | Format-Table -AutoSize
if(@($summary | Where-Object {$_.critical -gt 0 -or $_.high -gt 0}).Count -gt 0){throw 'High/Critical findings require review before release. See JSON reports.'}
