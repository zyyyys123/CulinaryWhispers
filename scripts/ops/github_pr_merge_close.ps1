$ErrorActionPreference = "Stop"

param(
  [Parameter(Mandatory = $true)]
  [int]$IssueNumber,

  [Parameter(Mandatory = $true)]
  [string]$HeadBranch,

  [string]$BaseBranch = "master",
  [string]$Repo = "zyyyys123/CulinaryWhispers",
  [ValidateSet("merge", "squash", "rebase")]
  [string]$MergeMethod = "squash",
  [string]$PrTitle,
  [string]$PrBodyPath
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

$issueUrl = "https://api.github.com/repos/$owner/$name/issues/$IssueNumber"
$issue = Invoke-GhApi "GET" $issueUrl $null

if (-not $PrTitle) { $PrTitle = [string]$issue.title }

$body = ""
if ($PrBodyPath) {
  $body = Get-Content -Raw -Encoding UTF8 -Path $PrBodyPath
} else {
  $issueBody = [string]$issue.body
  $body = @"
## 背景
- （来自 Issue #$IssueNumber）

## 问题
- （来自 Issue #$IssueNumber）

## 验收标准（来自 Issue）
$issueBody

## 解决方案
- 

## 最终结果
- 

## 测试与验证
- [ ] 本地构建通过（前端/后端）
- [ ] 核心路径手工验证通过

## 影响范围与风险
- 影响：
- 风险：
- 回滚/降级：

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

