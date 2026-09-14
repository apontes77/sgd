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

case "${JAVA_TOOL_OPTIONS:-}" in
  *MaxRAMPercentage*) ;;
  *)
    # Starter (~512Mi): keep heap ~half the cgroup so metaspace/native/Tomcat fit.
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -XX:MaxRAMPercentage=50.0 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Xss512k"
    ;;
esac

exec java -jar /app/app.jar
