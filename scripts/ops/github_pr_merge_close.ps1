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
  $b = if ($Background) { $Background } else { "（来自 Issue #$IssueNumber）" }
  $p = if ($Problem) { $Problem } else { "（来自 Issue #$IssueNumber）" }
  $a = if ($Acceptance) { $Acceptance } else { $issueBody }
  $s = if ($Solution) { $Solution } else { "" }
  $r = if ($Result) { $Result } else { "" }
  $v = if ($Verification) { $Verification } else { "- [ ] 本地构建通过（前端/后端）`n- [ ] 核心路径手工验证通过" }
  $k = if ($Risk) { $Risk } else { "- 影响：`n- 风险：`n- 回滚/降级：" }
  $body = @"
## 背景
- $b

## 问题
- $p

## 验收标准（来自 Issue）
$a

## 解决方案
$s

## 最终结果
$r

## 测试与验证
$v

## 影响范围与风险
$k

## 关联 Issue
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
