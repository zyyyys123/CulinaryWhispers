#!/bin/sh
set -e

if [ "${SKYWALKING_ENABLED}" = "true" ] && [ -f "/app/agent/skywalking-agent.jar" ]; then
  exec java -javaagent:/app/agent/skywalking-agent.jar -Dskywalking.agent.service_name=culinary-backend -Dskywalking.collector.backend_service=${SKYWALKING_BACKEND_SERVICE:-skywalking-oap:11800} -jar /app/app.jar
fi

exec java -jar /app/app.jar

