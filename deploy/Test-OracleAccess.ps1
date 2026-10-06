param([string]$BaseUrl='https://dazbones.161-33-178-229.sslip.io',
      [string]$CodesFile="$PSScriptRoot/../backups/oracle-credentials/site-login-codes.txt")
$ErrorActionPreference='Stop'
$codes=ConvertFrom-StringData (Get-Content -Raw -LiteralPath $CodesFile)
foreach($role in @('ADMIN_CODE','EDITOR_CODE')) {
  $page=Invoke-WebRequest "$BaseUrl/login" -SessionVariable session
  $token=[regex]::Match($page.Content,'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
  if(!$token){throw 'Missing CSRF token'}
  $null=Invoke-WebRequest "$BaseUrl/login" -Method Post -WebSession $session -Body @{code=$codes[$role];_csrf=$token}
  $null=Invoke-RestMethod "$BaseUrl/api/input?year=2026&month=2026-10" -WebSession $session
  foreach($path in @('/fee','/gear','/survey/attendance','/players/stats')) {
    $response=Invoke-WebRequest "$BaseUrl$path" -WebSession $session
    if($response.Content -notmatch 'data-csrf='){throw "Input view check failed: $path"}
  }
  $admin=Invoke-WebRequest "$BaseUrl/admin/player-settings" -WebSession $session -SkipHttpErrorCheck
  $expected=if($role -eq 'ADMIN_CODE'){200}else{403}
  if([int]$admin.StatusCode -ne $expected){throw 'Role restriction check failed'}
  Write-Output "$role login and page permissions: PASS"
}
$anonymous=Invoke-WebRequest "$BaseUrl/api/input?year=2026&month=2026-10" -SkipHttpErrorCheck
if([int]$anonymous.StatusCode -ne 401){throw 'Anonymous API access was not blocked'}
$homeResponse=Invoke-WebRequest "$BaseUrl/"
if($homeResponse.Content -notmatch 'DVETcRUkxMO'){throw 'Migrated Instagram post missing'}
$photo=Invoke-WebRequest "$BaseUrl/uploads/images/group-photo.jpg"
if($photo.RawContentLength -lt 10000){throw 'Group photo missing'}
Write-Output 'Anonymous restrictions, Instagram and photo: PASS'
