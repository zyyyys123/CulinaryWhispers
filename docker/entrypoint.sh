#!/bin/sh
set -e

# 配置 JVM 崩溃日志和重放日志的存储路径（规范化）
JVM_OPTS="-XX:ErrorFile=/app/logs/jvm_crash/hs_err_pid%p.log -XX:ReplayDataFile=/app/logs/jvm_crash/replay_pid%p.log"

if [ "${SKYWALKING_ENABLED}" = "true" ] && [ -f "/app/agent/skywalking-agent.jar" ]; then
  exec java ${JVM_OPTS} -javaagent:/app/agent/skywalking-agent.jar -Dskywalking.agent.service_name=culinary-backend -Dskywalking.collector.backend_service=${SKYWALKING_BACKEND_SERVICE:-skywalking-oap:11800} -jar /app/app.jar
fi

exec java ${JVM_OPTS} -jar /app/app.jar

