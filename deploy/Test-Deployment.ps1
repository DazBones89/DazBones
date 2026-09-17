param([string]$WorkDirectory="$PSScriptRoot/../build/deployment-check")
$ErrorActionPreference='Stop'
$work=[IO.Path]::GetFullPath($WorkDirectory)
[IO.Directory]::CreateDirectory($work) | Out-Null
$tag=[guid]::NewGuid().ToString('N').Substring(0,8)
$sourceProject="dazbones-check-$tag"
$restoreProject="dazbones-restore-$tag"
$sourceEnv=Join-Path $work "source-$tag.env"
$restoreEnv=Join-Path $work "restore-$tag.env"
& "$PSScriptRoot/Initialize-Environment.ps1" -SiteHost 'http://localhost' -Destination $sourceEnv
& "$PSScriptRoot/Initialize-Environment.ps1" -SiteHost 'http://localhost' -Destination $restoreEnv
foreach($entry in @(@($sourceEnv,19080,19443),@($restoreEnv,19081,19444))){
    $text=[IO.File]::ReadAllText($entry[0]).Replace('HTTP_PORT=80',"HTTP_PORT=$($entry[1])").Replace('HTTPS_PORT=443',"HTTPS_PORT=$($entry[2])")
    [IO.File]::WriteAllText($entry[0],$text)
}
$override=Join-Path $work "override-$tag.yml"
@'
services:
  app:
    environment:
      SERVER_SERVLET_SESSION_COOKIE_SECURE: 'false'
'@ | Set-Content -LiteralPath $override -Encoding utf8
function Invoke-CheckCompose {
    param([string]$Project,[string]$Environment,[string[]]$Arguments)
    & docker compose --project-name $Project --env-file $Environment -f "$PSScriptRoot/compose.yml" -f $override @Arguments
    if($LASTEXITCODE -ne 0){throw 'Deployment check command failed'}
}
function Wait-Ready([int]$Port){
    $deadline=(Get-Date).AddMinutes(3)
    do {
        try { if((Invoke-RestMethod "http://localhost:$Port/health/readiness" -TimeoutSec 3).status -eq 'UP'){return} }catch{}
        Start-Sleep -Seconds 2
    }while((Get-Date) -lt $deadline)
    throw 'Site did not become ready'
}
try {
    Invoke-CheckCompose $sourceProject $sourceEnv @('up','-d','--no-build')
    Wait-Ready 19080
    $sql="INSERT INTO players(name,at_bats,hits,delete_flg) VALUES ('deployment-check',10,3,0);"
    Invoke-CheckCompose $sourceProject $sourceEnv @('exec','-T','db','sh','-c',('MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "'+$sql+'"'))
    Invoke-CheckCompose $sourceProject $sourceEnv @('exec','-T','app','sh','-c','printf restore-test > /data/images/restore-check.txt')
    $config=ConvertFrom-StringData (Get-Content -LiteralPath $sourceEnv -Raw)
    $login=Invoke-WebRequest 'http://localhost:19080/login' -SessionVariable loginSession
    $token=[regex]::Match($login.Content,'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
    $null=Invoke-WebRequest 'http://localhost:19080/login' -Method Post -WebSession $loginSession -Body @{code=$config.ADMIN_CODE;_csrf=$token}
    $page=Invoke-WebRequest 'http://localhost:19080/survey/attendance' -WebSession $loginSession
    if(!$page.Content.Contains('deployment-check')){throw 'Fresh database login/roster check failed'}
    & "$PSScriptRoot/Backup.ps1" -ProjectName $sourceProject -EnvFile $sourceEnv -OutputRoot (Join-Path $work "backups-$tag")
    $backup=(Get-ChildItem -LiteralPath (Join-Path $work "backups-$tag") -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
    Invoke-CheckCompose $restoreProject $restoreEnv @('up','-d','--wait','db')
    & "$PSScriptRoot/Restore.ps1" -BackupDirectory $backup -ProjectName $restoreProject -EnvFile $restoreEnv -ConfirmReplace
    Invoke-CheckCompose $restoreProject $restoreEnv @('up','-d','--no-build')
    Wait-Ready 19081
    $restored=Invoke-WebRequest 'http://localhost:19081/players'
    if(!$restored.Content.Contains('deployment-check')){throw 'Restored DB row is missing'}
    $image=Invoke-WebRequest 'http://localhost:19081/uploads/images/restore-check.txt'
    $imageText=if($image.Content -is [byte[]]){[Text.Encoding]::UTF8.GetString($image.Content)}else{[string]$image.Content}
    if($imageText -ne 'restore-test'){throw 'Restored upload is missing or corrupted'}
    $versions=Invoke-CheckCompose $restoreProject $restoreEnv @('exec','-T','db','sh','-c','MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "SELECT version FROM flyway_schema_history WHERE success=1 ORDER BY installed_rank"')
    if(($versions -join ',') -ne '0,1,2,3'){throw 'Unexpected migration history'}
    Write-Output 'PASS: Docker build runtime, blank DB migrations, login, roster, DB backup/restore and image backup/restore.'
} finally {
    # These two random project names are created only by this isolated test; never touch the user's existing project.
    & docker compose --project-name $sourceProject --env-file $sourceEnv -f "$PSScriptRoot/compose.yml" down --volumes
    & docker compose --project-name $restoreProject --env-file $restoreEnv -f "$PSScriptRoot/compose.yml" down --volumes
}
