# Build Stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests to speed up build and avoid environment dependency issues during build
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# 下载 SkyWalking Agent (模拟，实际应从官网下载并解压)
# 生产环境通常使用 Sidecar 模式或将 Agent 打包到基础镜像中
# 这里为了演示 Dockerfile 逻辑补充，添加下载步骤
RUN curl -L https://archive.apache.org/dist/skywalking/java-agent/9.0.0/apache-skywalking-java-agent-9.0.0.tgz -o agent.tgz \
    && tar -zxvf agent.tgz \
    && rm agent.tgz \
    && mv skywalking-agent agent

EXPOSE 8080
# 启用 SkyWalking Agent
ENTRYPOINT ["java", "-javaagent:/app/agent/skywalking-agent.jar", "-Dskywalking.agent.service_name=culinary-backend", "-Dskywalking.collector.backend_service=skywalking-oap:11800", "-jar", "app.jar"]
