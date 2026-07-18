#!/bin/sh
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
  database_host_and_path=${DATABASE_URL#*://}
  database_host_and_path=${database_host_and_path#*@}
  database_authority=${database_host_and_path%%/*}

  case "$database_authority" in
    *:*)
      export DB_HOST=${database_authority%:*}
      export DB_PORT=${database_authority##*:}
      ;;
    *)
      export DB_HOST=$database_authority
      export DB_PORT=5432
      ;;
  esac
fi

resource_attributes="deployment.environment.name=${DEPLOYMENT_ENVIRONMENT:-local}"
if [ -n "${RENDER_GIT_COMMIT:-}" ]; then
  resource_attributes="${resource_attributes},service.version=${RENDER_GIT_COMMIT}"
fi
if [ -n "${OTEL_RESOURCE_ATTRIBUTES:-}" ]; then
  resource_attributes="${resource_attributes},${OTEL_RESOURCE_ATTRIBUTES}"
fi
export OTEL_RESOURCE_ATTRIBUTES=$resource_attributes

if [ -z "${OTEL_EXPORTER_OTLP_ENDPOINT:-}" ]; then
  export OTEL_TRACES_EXPORTER=${OTEL_TRACES_EXPORTER:-none}
  export OTEL_METRICS_EXPORTER=${OTEL_METRICS_EXPORTER:-none}
  export OTEL_LOGS_EXPORTER=${OTEL_LOGS_EXPORTER:-none}
fi

export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -javaagent:/otel/opentelemetry-javaagent.jar -XX:MaxRAMPercentage=70.0"

exec java -jar /app/app.jar
