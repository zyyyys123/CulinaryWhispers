$ErrorActionPreference = "Stop"

$jdkPath = Resolve-Path ".tools\jdk\jdk-21.0.10+7"
$env:JAVA_HOME = $jdkPath.Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$argsStr = @(
  "--server.port=8080",
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
) -join " "

& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=$argsStr"
