param([Parameter(Mandatory)][string]$SiteHost,[string]$Destination="$PSScriptRoot/.env")
$ErrorActionPreference='Stop'
if(Test-Path -LiteralPath $Destination){throw 'Environment file already exists; it will not be overwritten.'}
if($SiteHost -notmatch '^(http://localhost|[a-zA-Z0-9.-]+)$'){throw 'Specify a domain name, or http://localhost for an isolated local check.'}
function New-Code {
    $bytes=New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
}
@("SITE_HOST=$SiteHost","DB_PASSWORD=$(New-Code)","DB_ROOT_PASSWORD=$(New-Code)","ADMIN_CODE=$(New-Code)","EDITOR_CODE=$(New-Code)",'HTTP_PORT=80','HTTPS_PORT=443') | Set-Content -LiteralPath $Destination -Encoding utf8
Write-Output 'Created environment file with fresh codes. Keep it private and store a separate secure copy.'
