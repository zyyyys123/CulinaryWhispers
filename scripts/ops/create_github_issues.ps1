$ErrorActionPreference = "Stop"

$repo = "zyyyys123/CulinaryWhispers"
$csvPath = "D:\MyCollegeProject\CulinaryWhispers\docs2.0\plan2.0\github_issues.csv"

function Resolve-Gh {
  $cmd = Get-Command gh -ErrorAction SilentlyContinue
  if ($cmd) { return $cmd.Path }
  $candidates = @(
    $env:CW_GH_PATH,
    "C:\Program Files\GitHub CLI\gh.exe",
    "$env:LOCALAPPDATA\Programs\GitHub CLI\gh.exe",
    "$env:LOCALAPPDATA\Programs\GitHub CLI\bin\gh.exe",
    "$env:LOCALAPPDATA\Microsoft\WinGet\Links\gh.exe"
  ) | Where-Object { $_ -and (Test-Path $_) }
  if ($candidates.Count -gt 0) { return $candidates[0] }
  return $null
}

$ghPath = Resolve-Gh
if (-not $ghPath) {
  throw "GitHub CLI (gh) not found in PATH. Reopen terminal/IDE, or set CW_GH_PATH to gh.exe path. Install: https://cli.github.com/"
}

$issues = Import-Csv -Path $csvPath
if (-not $issues -or $issues.Count -eq 0) {
  throw "No issues found in CSV: $csvPath"
}

foreach ($it in $issues) {
  $title = [string]$it.title
  $body = [string]$it.body
  $labelsRaw = [string]$it.labels

  $args = @("issue", "create", "--repo", $repo, "--title", $title, "--body", $body)
  if ($labelsRaw) {
    $labels = $labelsRaw.Split("|") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    foreach ($lb in $labels) {
      $args += @("--label", $lb)
    }
  }

  & $ghPath @args
}
