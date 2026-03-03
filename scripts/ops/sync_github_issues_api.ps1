$ErrorActionPreference = "Stop"

$repo = "zyyyys123/CulinaryWhispers"
$csvPath = "D:\MyCollegeProject\CulinaryWhispers\docs2.0\plan2.0\github_issues.csv"

$token = $env:GITHUB_TOKEN
if (-not $token) { $token = $env:GH_TOKEN }
if (-not $token) {
  throw "Missing token. Set GITHUB_TOKEN (or GH_TOKEN) with repo issues permission."
}

function Convert-LiteralEscapes($s) {
  if ($null -eq $s) { return "" }
  $t = [string]$s
  $t = $t -replace "\\r\\n", "`r`n"
  $t = $t -replace "\\n", "`n"
  $t = $t -replace "\\t", "`t"
  return $t
}

$parts = $repo.Split("/")
if ($parts.Count -ne 2) { throw "Invalid repo format: $repo" }
$owner = $parts[0]
$name = $parts[1]

$csvRaw = Get-Content -Raw -Encoding UTF8 -Path $csvPath
$rows = $csvRaw | ConvertFrom-Csv
if (-not $rows -or $rows.Count -eq 0) { throw "No issues found in CSV: $csvPath" }

$offset = 1
if ($env:CW_ISSUE_NUMBER_OFFSET) {
  try { $offset = [int]$env:CW_ISSUE_NUMBER_OFFSET } catch { $offset = 1 }
}

$startAt = 1
if ($env:CW_ISSUE_START_AT) {
  try { $startAt = [int]$env:CW_ISSUE_START_AT } catch { $startAt = 1 }
  if ($startAt -lt 1) { $startAt = 1 }
}

$endAt = $rows.Count
if ($env:CW_ISSUE_END_AT) {
  try { $endAt = [int]$env:CW_ISSUE_END_AT } catch { $endAt = $rows.Count }
  if ($endAt -gt $rows.Count) { $endAt = $rows.Count }
}

$headers = @{
  Authorization = "Bearer $token"
  Accept        = "application/vnd.github+json"
  "X-GitHub-Api-Version" = "2022-11-28"
  "User-Agent"  = "CulinaryWhispers-IssueSeeder"
}

$i = 0
foreach ($r in $rows) {
  $i++
  if ($i -lt $startAt -or $i -gt $endAt) { continue }

  $issueNumber = $offset + $i
  $title = Convert-LiteralEscapes $r.title
  $body = Convert-LiteralEscapes $r.body
  $labelsRaw = [string]$r.labels

  $labels = @()
  if ($labelsRaw) {
    $labels = $labelsRaw.Split("|") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
  }

  $payload = @{
    title  = $title
    body   = $body
    labels = $labels
  } | ConvertTo-Json -Depth 6 -Compress

  $url = "https://api.github.com/repos/$owner/$name/issues/$issueNumber"
  $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
  $resp = Invoke-RestMethod -Method Patch -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes
  Write-Output $resp.html_url
  Start-Sleep -Milliseconds 200
}

