[CmdletBinding()]
param(
  [string]$Repo = "zyyyys123/CulinaryWhispers",
  [string]$HeadBranch = "fix/auth-sync-comment-author",
  [string]$BaseBranch = "master",
  [string]$PrTitle = "fix(auth,social): sync auth state and return comment author",
  [string]$PrBodyPath = "",
  [int[]]$CloseIssues = @(27, 28),
  [ValidateSet("merge", "squash", "rebase")]
  [string]$MergeMethod = "squash",
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
if (-not $PrBodyPath) {
  $PrBodyPath = Join-Path $scriptRoot "pr_body_fix_auth_sync_comment_author.md"
} elseif (-not (Split-Path -Path $PrBodyPath -IsAbsolute)) {
  $PrBodyPath = Join-Path (Get-Location).Path $PrBodyPath
}

Write-Host "[PRFlow] Repo=$Repo Base=$BaseBranch Head=$HeadBranch MergeMethod=$MergeMethod DryRun=$($DryRun.IsPresent)"

$findUrl = ""
if ($Repo) {
  $repoParts = $Repo.Split("/")
  if ($repoParts.Count -eq 2) {
    $findUrl = "https://api.github.com/repos/$($repoParts[0])/$($repoParts[1])/pulls?state=open&head=$($repoParts[0])`:$HeadBranch&base=$BaseBranch"
  }
}

if ($DryRun) {
  Write-Host "[PRFlow] DryRun enabled: will not call GitHub API."
  if ($PrBodyPath) {
    Write-Host "[PRFlow] PR body path (resolved): $PrBodyPath"
  }
  if ($findUrl) {
    Write-Host "[PRFlow] Would query: $findUrl"
  }
  Write-Host "[PRFlow] Done."
  exit 0
}

$token = $env:GITHUB_TOKEN
if (-not $token) { $token = $env:GH_TOKEN }
if (-not $token) {
  Write-Error "[PRFlow] Missing token. Set GITHUB_TOKEN (or GH_TOKEN) then re-run."
  exit 1
}

$parts = $Repo.Split("/")
if ($parts.Count -ne 2) {
  Write-Error "[PRFlow] Invalid repo format: $Repo (expected owner/name)"
  exit 1
}
$owner = $parts[0]
$name = $parts[1]

$headers = @{
  Authorization          = "Bearer $token"
  Accept                 = "application/vnd.github+json"
  "X-GitHub-Api-Version" = "2022-11-28"
  "User-Agent"           = "CulinaryWhispers-PRFlow"
}

function Invoke-GhApi($method, $url, $bodyObj) {
  try {
    if ($null -eq $bodyObj) {
      return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -TimeoutSec 60
    }
    $json = $bodyObj | ConvertTo-Json -Depth 10 -Compress
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes -TimeoutSec 60
  } catch {
    $msg = $_.Exception.Message
    Write-Error "[PRFlow] GitHub API failed: $method $url"
    Write-Error "[PRFlow] $msg"
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      Write-Error "[PRFlow] StatusCode: $($_.Exception.Response.StatusCode.value__)"
    }
    throw
  }
}

$body = ""
if (Test-Path -Path $PrBodyPath) {
  $body = Get-Content -Raw -Encoding UTF8 -Path $PrBodyPath
  Write-Host "[PRFlow] Loaded PR body from: $PrBodyPath"
} else {
  Write-Host "[PRFlow] PR body file not found, creating PR without body: $PrBodyPath"
}

Write-Host "[PRFlow] Looking for existing open PR..."

$existing = Invoke-GhApi "GET" $findUrl $null
$existingList = @()
if ($existing) { $existingList = @($existing) }

$pr = $null
if ($existingList.Count -gt 0) {
  $pr = $existingList[0]
  Write-Host "[PRFlow] Found existing PR #$($pr.number): $($pr.html_url)"
} else {
  $createUrl = "https://api.github.com/repos/$owner/$name/pulls"
  Write-Host "[PRFlow] Creating PR..."
  $pr = Invoke-GhApi "POST" $createUrl @{
    title = $PrTitle
    head  = $HeadBranch
    base  = $BaseBranch
    body  = $body
    draft = $false
  }
  Write-Host "[PRFlow] Created PR #$($pr.number): $($pr.html_url)"
}

$mergeUrl = "https://api.github.com/repos/$owner/$name/pulls/$($pr.number)/merge"
Write-Host "[PRFlow] Merging PR #$($pr.number)..."
$mergeRes = Invoke-GhApi "PUT" $mergeUrl @{
  merge_method = $MergeMethod
}
Write-Host "[PRFlow] Merge result: merged=$($mergeRes.merged) sha=$($mergeRes.sha)"

foreach ($n in $CloseIssues) {
  if ($n -le 0) { continue }
  $issueUrl = "https://api.github.com/repos/$owner/$name/issues/$n"
  try {
    Invoke-GhApi "PATCH" $issueUrl @{ state = "closed" } | Out-Null
    Write-Host "[PRFlow] Closed issue #$n"
  } catch {
    Write-Host "[PRFlow] Failed to close issue #$n (ignored)"
  }
}

Write-Host "[PRFlow] Done."
Write-Output $pr.html_url
