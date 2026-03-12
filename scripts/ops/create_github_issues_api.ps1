$ErrorActionPreference = "Stop"

$repo = "zyyyys123/CulinaryWhispers"
$csvPath = "D:\MyCollegeProject\CulinaryWhispers\docs2.0\plan2.0\github_issues.csv"

if ($env:CW_ISSUE_REPO) { $repo = $env:CW_ISSUE_REPO }
if ($env:CW_ISSUE_CSV_PATH) { $csvPath = $env:CW_ISSUE_CSV_PATH }

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

$autoCreateLabels = $false
if ($env:CW_ISSUE_AUTO_CREATE_LABELS) {
  $v = ([string]$env:CW_ISSUE_AUTO_CREATE_LABELS).Trim().ToLowerInvariant()
  if ($v -eq "1" -or $v -eq "true" -or $v -eq "yes") { $autoCreateLabels = $true }
}

function Get-AllRepoLabels {
  $all = @()
  $page = 1
  while ($true) {
    $u = "https://api.github.com/repos/$owner/$name/labels?per_page=100&page=$page"
    $resp = Invoke-RestMethod -Method Get -Uri $u -Headers $headers -TimeoutSec 60
    if (-not $resp -or $resp.Count -eq 0) { break }
    $all += $resp
    if ($resp.Count -lt 100) { break }
    $page++
    if ($page -gt 50) { break }
  }
  return $all
}

function New-LabelColor($labelName) {
  $n = ([string]$labelName).ToLowerInvariant()
  if ($n.StartsWith("area:frontend")) { return "1d76db" }
  if ($n.StartsWith("area:backend")) { return "0e8a16" }
  if ($n.StartsWith("area:security")) { return "b60205" }
  if ($n.StartsWith("topic:")) { return "5319e7" }
  if ($n.StartsWith("type:bug")) { return "d73a4a" }
  if ($n.StartsWith("type:feature")) { return "0e8a16" }
  if ($n.StartsWith("type:chore")) { return "cfd3d7" }
  if ($n.StartsWith("type:ui")) { return "a2eeef" }
  return "ededed"
}

function Ensure-LabelsExist($labels, $knownSet) {
  if (-not $labels -or $labels.Count -eq 0) { return }
  foreach ($lb in $labels) {
    $name0 = ([string]$lb).Trim()
    if (-not $name0) { continue }
    $key = $name0.ToLowerInvariant()
    if ($knownSet.Contains($key)) { continue }

    $createUrl = "https://api.github.com/repos/$owner/$name/labels"
    $payload = @{
      name  = $name0
      color = (New-LabelColor $name0)
    } | ConvertTo-Json -Depth 4 -Compress

    try {
      Invoke-RestMethod -Method Post -Uri $createUrl -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($payload)) -TimeoutSec 60 | Out-Null
      $knownSet.Add($key) | Out-Null
    } catch {
      try {
        $knownSet.Add($key) | Out-Null
      } catch {}
    }
  }
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

$knownLabels = [System.Collections.Generic.HashSet[string]]::new()
if ($autoCreateLabels) {
  try {
    $existing = Get-AllRepoLabels
    foreach ($x in $existing) {
      $nm = ([string]$x.name).Trim()
      if ($nm) { $knownLabels.Add($nm.ToLowerInvariant()) | Out-Null }
    }
  } catch {
  }
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

  if ($autoCreateLabels) {
    Ensure-LabelsExist $labels $knownLabels
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
