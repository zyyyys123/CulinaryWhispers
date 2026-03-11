$ErrorActionPreference = "Stop"

param(
  [int]$IssueNumber = 0,

  [Parameter(Mandatory = $true)]
  [string]$HeadBranch,

  [string]$BaseBranch = "master",
  [string]$Repo = "zyyyys123/CulinaryWhispers",
  [ValidateSet("merge", "squash", "rebase")]
  [string]$MergeMethod = "squash",
  [string]$PrTitle,
  [string]$PrBodyPath,
  [string]$CreateIssueTitle,
  [string]$CreateIssueBody,
  [string[]]$CreateIssueLabels = @(),
  [string]$Background,
  [string]$Problem,
  [string]$Acceptance,
  [string]$Solution,
  [string]$Result,
  [string]$Verification,
  [string]$Risk
)

$token = $env:GITHUB_TOKEN
if (-not $token) { $token = $env:GH_TOKEN }
if (-not $token) { throw "Missing token. Set GITHUB_TOKEN (or GH_TOKEN)." }

$parts = $Repo.Split("/")
if ($parts.Count -ne 2) { throw "Invalid repo format: $Repo" }
$owner = $parts[0]
$name = $parts[1]

$headers = @{
  Authorization         = "Bearer $token"
  Accept                = "application/vnd.github+json"
  "X-GitHub-Api-Version" = "2022-11-28"
  "User-Agent"          = "CulinaryWhispers-PRFlow"
}

function Invoke-GhApi($method, $url, $bodyObj) {
  if ($null -eq $bodyObj) {
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -TimeoutSec 60
  }
  $json = $bodyObj | ConvertTo-Json -Depth 10 -Compress
  $bytes = [Text.Encoding]::UTF8.GetBytes($json)
  return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes -TimeoutSec 60
}

$createPrIssueFromParams = $IssueNumber -le 0
if ($createPrIssueFromParams) {
  if (-not $CreateIssueTitle) {
    throw "IssueNumber is missing. Provide -IssueNumber or use -CreateIssueTitle to create an issue."
  }
  $createIssueUrl = "https://api.github.com/repos/$owner/$name/issues"
  $issuePayload = @{
    title = $CreateIssueTitle
    body  = $CreateIssueBody
  }
  if ($CreateIssueLabels -and $CreateIssueLabels.Count -gt 0) {
    $issuePayload.labels = $CreateIssueLabels
  }
  $createdIssue = Invoke-GhApi "POST" $createIssueUrl $issuePayload
  $IssueNumber = [int]$createdIssue.number
}

$issueUrl = "https://api.github.com/repos/$owner/$name/issues/$IssueNumber"
$issue = Invoke-GhApi "GET" $issueUrl $null

if (-not $PrTitle) { $PrTitle = [string]$issue.title }

$body = ""
if ($PrBodyPath) {
  $body = Get-Content -Raw -Encoding UTF8 -Path $PrBodyPath
} else {
  $issueBody = [string]$issue.body
  $b = if ($Background) { $Background } else { "(from Issue #$IssueNumber)" }
  $p = if ($Problem) { $Problem } else { "(from Issue #$IssueNumber)" }
  $a = if ($Acceptance) { $Acceptance } else { $issueBody }
  $s = if ($Solution) { $Solution } else { "" }
  $r = if ($Result) { $Result } else { "" }
  $v = if ($Verification) { $Verification } else { "- [ ] Local build passed (frontend/backend)`n- [ ] Manual smoke test passed" }
  $k = if ($Risk) { $Risk } else { "- Impact:`n- Risk:`n- Rollback:" }
  $body = @"
## Background
- $b

## Problem
- $p

## Acceptance Criteria (from Issue)
$a

## Solution
$s

## Result
$r

## Verification
$v

## Impact & Risk
$k

## Related Issue
- Closes #$IssueNumber
"@
}

$createPrUrl = "https://api.github.com/repos/$owner/$name/pulls"
$pr = Invoke-GhApi "POST" $createPrUrl @{
  title = $PrTitle
  head  = $HeadBranch
  base  = $BaseBranch
  body  = $body
  draft = $false
}

$mergeUrl = "https://api.github.com/repos/$owner/$name/pulls/$($pr.number)/merge"
Invoke-GhApi "PUT" $mergeUrl @{
  merge_method = $MergeMethod
}

$closed = Invoke-GhApi "PATCH" $issueUrl @{
  state = "closed"
}

Write-Output $pr.html_url
Write-Output $closed.html_url
