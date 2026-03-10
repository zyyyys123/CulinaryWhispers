$ErrorActionPreference = "Stop"

param(
  [string]$Repo = "zyyyys123/CulinaryWhispers",
  [string]$HeadBranch = "fix/auth-sync-comment-author",
  [string]$BaseBranch = "master",
  [string]$PrTitle = "fix(auth,social): sync auth state and return comment author",
  [string]$PrBodyPath = "scripts/ops/pr_body_fix_auth_sync_comment_author.md",
  [int[]]$CloseIssues = @(27, 28),
  [ValidateSet("merge", "squash", "rebase")]
  [string]$MergeMethod = "squash"
)

$token = $env:GITHUB_TOKEN
if (-not $token) { $token = $env:GH_TOKEN }
if (-not $token) { throw "Missing token. Set GITHUB_TOKEN (or GH_TOKEN)." }

$parts = $Repo.Split("/")
if ($parts.Count -ne 2) { throw "Invalid repo format: $Repo" }
$owner = $parts[0]
$name = $parts[1]

$headers = @{
  Authorization          = "Bearer $token"
  Accept                 = "application/vnd.github+json"
  "X-GitHub-Api-Version" = "2022-11-28"
  "User-Agent"           = "CulinaryWhispers-PRFlow"
}

function Invoke-GhApi($method, $url, $bodyObj) {
  if ($null -eq $bodyObj) {
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -TimeoutSec 60
  }
  $json = $bodyObj | ConvertTo-Json -Depth 10 -Compress
  $bytes = [Text.Encoding]::UTF8.GetBytes($json)
  return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes -TimeoutSec 60
}

$body = ""
if (Test-Path -Path $PrBodyPath) {
  $body = Get-Content -Raw -Encoding UTF8 -Path $PrBodyPath
}

$findUrl = "https://api.github.com/repos/$owner/$name/pulls?state=open&head=$owner`:$HeadBranch&base=$BaseBranch"
$existing = Invoke-GhApi "GET" $findUrl $null

$pr = $null
if ($existing -and $existing.Count -gt 0) {
  $pr = $existing[0]
} else {
  $createUrl = "https://api.github.com/repos/$owner/$name/pulls"
  $pr = Invoke-GhApi "POST" $createUrl @{
    title = $PrTitle
    head  = $HeadBranch
    base  = $BaseBranch
    body  = $body
    draft = $false
  }
}

$mergeUrl = "https://api.github.com/repos/$owner/$name/pulls/$($pr.number)/merge"
Invoke-GhApi "PUT" $mergeUrl @{
  merge_method = $MergeMethod
}

foreach ($n in $CloseIssues) {
  if ($n -le 0) { continue }
  $issueUrl = "https://api.github.com/repos/$owner/$name/issues/$n"
  try {
    Invoke-GhApi "PATCH" $issueUrl @{ state = "closed" } | Out-Null
  } catch {
  }
}

Write-Output $pr.html_url

