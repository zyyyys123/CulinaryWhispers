$ErrorActionPreference = "Stop"

param(
  [Parameter(Mandatory = $true)]
  [int]$IssueNumber,

  [string]$Repo = "zyyyys123/CulinaryWhispers",
  [string]$CommentPath
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
  "User-Agent"          = "CulinaryWhispers-IssueCloser"
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

$comment = ""
if ($CommentPath) {
  $comment = Get-Content -Raw -Encoding UTF8 -Path $CommentPath
} else {
  $comment = @"
## 背景
- （补充）

## 问题
- （补充）

## 验收标准（来自 Issue）
$([string]$issue.body)

## 解决方案
- （补充）

## 最终结果
- （补充）

## 关联变更
- （补充 commit / PR 链接）
"@
}

$commentUrl = "https://api.github.com/repos/$owner/$name/issues/$IssueNumber/comments"
Invoke-GhApi "POST" $commentUrl @{ body = $comment }

$closed = Invoke-GhApi "PATCH" $issueUrl @{ state = "closed" }

Write-Output $closed.html_url

