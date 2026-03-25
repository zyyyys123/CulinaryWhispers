param(
  [string]$DbUser = "root",
  [string]$DbPassword = "",
  [string]$DbName = "culinary_user",
  [string]$MySqlService = "mysql",
  [string]$MealdbSqlPath = "",
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $MealdbSqlPath) {
  $MealdbSqlPath = Join-Path $repoRoot "sql\mealdb_recipes.sql"
} elseif (-not (Split-Path -Path $MealdbSqlPath -IsAbsolute)) {
  $MealdbSqlPath = Join-Path $repoRoot $MealdbSqlPath
}

if (-not (Test-Path -Path $MealdbSqlPath)) {
  throw "Mealdb SQL file not found: $MealdbSqlPath"
}

if (-not $DbPassword) {
  $DbPassword = $env:CW_DB_PASSWORD
}
if (-not $DbPassword) {
  $DbPassword = $env:MYSQL_ROOT_PASSWORD
}
if (-not $DbPassword) {
  $DbPassword = "root"
}

$pairs = @{}
$regex = [regex]::new("REPLACE\s+INTO\s+t_rcp_info.*?VALUES\s*\(\s*\d+\s*,\s*\d+\s*,\s*'[^']*'\s*,\s*'(?<cover>[^']*)'\s*,\s*'(?<video>[^']*)'", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
$videoAllow = [regex]::new("^https?://(www\.)?(youtube\.com|youtu\.be)/", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)

Get-Content -Path $MealdbSqlPath -Encoding UTF8 | ForEach-Object {
  $line = $_
  if (-not $line) { return }
  $m = $regex.Match($line)
  if (-not $m.Success) { return }
  $cover = $m.Groups["cover"].Value
  $video = $m.Groups["video"].Value
  if (-not $cover) { return }
  if (-not $video) { return }
  if (-not $videoAllow.IsMatch($video)) { return }
  if (-not $pairs.ContainsKey($cover)) { $pairs[$cover] = $video }
}

if ($pairs.Count -eq 0) {
  throw "No (cover_url, video_url) pairs parsed from $MealdbSqlPath"
}

Write-Host ("[VideoBackfill] Parsed pairs: {0}" -f $pairs.Count)

$sqlLines = New-Object System.Collections.ArrayList
[void]$sqlLines.Add("USE $DbName;")
[void]$sqlLines.Add("DROP TABLE IF EXISTS tmp_mealdb_video;")
[void]$sqlLines.Add("CREATE TABLE tmp_mealdb_video (cover_url varchar(512) NOT NULL PRIMARY KEY, video_url varchar(512) DEFAULT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;")

$batch = New-Object System.Collections.ArrayList
$i = 0
foreach ($kv in $pairs.GetEnumerator()) {
  $cover = $kv.Key
  $videoRaw = $kv.Value
  if ($null -eq $videoRaw) { $videoRaw = "" }

  $cover = $cover.Replace('\', '\\').Replace("'", "''")
  $video = $videoRaw.Replace('\', '\\').Replace("'", "''")
  [void]$batch.Add("('$cover', '$video')")
  $i++
  if ($batch.Count -ge 200) {
    [void]$sqlLines.Add("INSERT INTO tmp_mealdb_video (cover_url, video_url) VALUES " + ($batch -join ",") + ";")
    $batch.Clear() | Out-Null
  }
}
if ($batch.Count -gt 0) {
  [void]$sqlLines.Add("INSERT INTO tmp_mealdb_video (cover_url, video_url) VALUES " + ($batch -join ",") + ";")
}

[void]$sqlLines.Add("SELECT COUNT(*) AS total, SUM(CASE WHEN video_url IS NOT NULL AND video_url<>'' THEN 1 ELSE 0 END) AS with_video_before FROM t_rcp_info;")
[void]$sqlLines.Add("UPDATE t_rcp_info r JOIN tmp_mealdb_video m ON r.cover_url = m.cover_url SET r.video_url = m.video_url WHERE (r.video_url IS NULL OR r.video_url='') AND m.video_url IS NOT NULL AND m.video_url<>'';")
[void]$sqlLines.Add("UPDATE t_rcp_step s JOIN t_rcp_info r ON s.recipe_id = r.id JOIN tmp_mealdb_video m ON r.cover_url = m.cover_url SET s.video_url = m.video_url WHERE s.step_no = 1 AND (s.video_url IS NULL OR s.video_url='') AND m.video_url IS NOT NULL AND m.video_url<>'';")
[void]$sqlLines.Add("SELECT COUNT(*) AS total, SUM(CASE WHEN video_url IS NOT NULL AND video_url<>'' THEN 1 ELSE 0 END) AS with_video_after FROM t_rcp_info;")
[void]$sqlLines.Add("DROP TABLE IF EXISTS tmp_mealdb_video;")

$sql = ($sqlLines -join "`n") + "`n"

if ($DryRun) {
  Write-Host "[VideoBackfill] DryRun enabled. First 30 lines of generated SQL:"
  ($sqlLines | Select-Object -First 30) | ForEach-Object { Write-Host $_ }
  exit 0
}

Write-Host "[VideoBackfill] Applying SQL to MySQL container..."
$sql | & docker compose exec -T $MySqlService mysql "-u$($DbUser)" "-p$($DbPassword)" "-D" "$DbName"
if ($LASTEXITCODE -ne 0) {
  throw "Video backfill failed. ExitCode=$LASTEXITCODE"
}
