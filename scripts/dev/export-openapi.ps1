$ErrorActionPreference = "Stop"

$jdkPath = Resolve-Path ".tools\jdk\jdk-21.0.10+7"
$env:JAVA_HOME = $jdkPath.Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$outDir = "docs\fronted\openapi"
$outFile = Join-Path $outDir "openapi.json"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

& .\mvnw.cmd "-q" "-DskipTests" "-Djacoco.skip=true" "package"
if ($LASTEXITCODE -ne 0) {
  throw "mvn package failed with exit code $LASTEXITCODE"
}

$jar = Get-ChildItem -Path "target" -Filter "*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) {
  throw "No jar found under target/"
}

$port = 8085
$args = @(
  "-jar", $jar.FullName,
  "--server.port=$port",
  "--cw.token.store=memory",
  "--cw.search.type=db",
  "--spring.task.scheduling.enabled=false",
  "--spring.datasource.driver-class-name=org.h2.Driver",
  "--spring.datasource.url=jdbc:h2:mem:openapi;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
  "--spring.datasource.username=sa",
  "--spring.datasource.password=",
  "--spring.sql.init.mode=never",
  "--spring.data.redis.host=localhost",
  "--spring.data.redis.port=6379",
  "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration"
)

$proc = Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList $args -PassThru -WindowStyle Hidden

try {
  $healthUrl = "http://localhost:$port/actuator/health"
  $deadline = (Get-Date).AddSeconds(90)
  while ((Get-Date) -lt $deadline) {
    try {
      $resp = Invoke-WebRequest -UseBasicParsing -Uri $healthUrl -TimeoutSec 3
      if ($resp.StatusCode -eq 200) { break }
    } catch {}
    Start-Sleep -Milliseconds 500
  }

  $docUrl = "http://localhost:$port/v3/api-docs"
  Invoke-WebRequest -UseBasicParsing -Uri $docUrl -TimeoutSec 30 -OutFile $outFile | Out-Null
  Write-Host "OpenAPI exported to $outFile"
} finally {
  if (!$proc.HasExited) {
    Stop-Process -Id $proc.Id -Force
  }
}
