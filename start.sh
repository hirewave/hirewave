#!/usr/bin/env bash
set -euo pipefail

JENKINS_CONFIG_ROOT="${JENKINS_CONFIG_ROOT:-/workspaces/jenkins_config}"

echo "[start] Using JENKINS_CONFIG_ROOT=$JENKINS_CONFIG_ROOT"
mkdir -p "$JENKINS_CONFIG_ROOT"

ENV_FILE="$(mktemp)"
trap 'rm -f "$ENV_FILE"' EXIT
printf 'JENKINS_CONFIG_ROOT=%s\n' "$JENKINS_CONFIG_ROOT" > "$ENV_FILE"

docker compose --env-file "$ENV_FILE" --profile mongo --profile prod-eng-service up -d
