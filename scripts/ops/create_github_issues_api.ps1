$ErrorActionPreference = "Stop"

$repo = "zyyyys123/CulinaryWhispers"
$csvPath = "D:\MyCollegeProject\CulinaryWhispers\docs2.0\plan2.0\github_issues.csv"

$token = $env:GITHUB_TOKEN
if (-not $token) { $token = $env:GH_TOKEN }
if (-not $token) {
  throw "Missing token. Set GITHUB_TOKEN (or GH_TOKEN) with repo issues permission."
}

if ($token.StartsWith("ghp_") -or $token.StartsWith("github_pat_")) {
} else {
  throw "Token format looks unexpected. Use a GitHub Personal Access Token."
}

$parts = $repo.Split("/")
if ($parts.Count -ne 2) { throw "Invalid repo format: $repo" }
$owner = $parts[0]
$name = $parts[1]

function Convert-LiteralEscapes($s) {
  if ($null -eq $s) { return "" }
  $t = [string]$s
  $t = $t -replace "\\r\\n", "`r`n"
  $t = $t -replace "\\n", "`n"
  $t = $t -replace "\\t", "`t"
  return $t
}

$csvRaw = Get-Content -Raw -Encoding UTF8 -Path $csvPath
$issues = $csvRaw | ConvertFrom-Csv
if (-not $issues -or $issues.Count -eq 0) {
  throw "No issues found in CSV: $csvPath"
}

$headers = @{
  Authorization = "Bearer $token"
  Accept        = "application/vnd.github+json"
  "X-GitHub-Api-Version" = "2022-11-28"
  "User-Agent"  = "CulinaryWhispers-IssueSeeder"
}

$maxRetries = 5
if ($env:CW_ISSUE_MAX_RETRIES) {
  try { $maxRetries = [int]$env:CW_ISSUE_MAX_RETRIES } catch { $maxRetries = 5 }
  if ($maxRetries -lt 1) { $maxRetries = 1 }
}

$startAt = 1
if ($env:CW_ISSUE_START_AT) {
  try { $startAt = [int]$env:CW_ISSUE_START_AT } catch { $startAt = 1 }
  if ($startAt -lt 1) { $startAt = 1 }
}

$i = 0
foreach ($it in $issues) {
  $i++
  if ($i -lt $startAt) { continue }
  $title = Convert-LiteralEscapes $it.title
  $body = Convert-LiteralEscapes $it.body
  $labelsRaw = [string]$it.labels

  $labels = @()
  if ($labelsRaw) {
    $labels = $labelsRaw.Split("|") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
  }

  $payload = @{
    title  = $title
    body   = $body
    labels = $labels
  } | ConvertTo-Json -Depth 6 -Compress

  $url = "https://api.github.com/repos/$owner/$name/issues"

  $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
  $attempt = 0
  while ($true) {
    $attempt++
    try {
      $resp = Invoke-RestMethod -Method Post -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body $bytes -TimeoutSec 60
      break
    } catch {
      if ($attempt -ge $maxRetries) {
        $snippet = $payload
        if ($snippet.Length -gt 500) { $snippet = $snippet.Substring(0, 500) + "..." }
        throw "Create issue failed at row ${i}: $title`nPayload snippet: $snippet`n$($_.Exception.Message)"
      }
      $delayMs = [int](800 * [math]::Pow(2, ($attempt - 1)))
      if ($delayMs -gt 15000) { $delayMs = 15000 }
      Start-Sleep -Milliseconds $delayMs
    }
  }

  Write-Output $resp.html_url
  Start-Sleep -Milliseconds 400
}
