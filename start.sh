#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REQUESTED_JENKINS_CONFIG_ROOT="${JENKINS_CONFIG_ROOT:-./jenkins_config}"
DOCKER_HUB_USERNAME="${DOCKER_HUB_USERNAME:-masacru}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

case "$REQUESTED_JENKINS_CONFIG_ROOT" in
	"$SCRIPT_DIR/jenkins_config"|"$SCRIPT_DIR/jenkins_config/")
		JENKINS_CONFIG_ROOT="./jenkins_config"
		;;
	/*)
		if [[ -n "${WSL_DISTRO_NAME:-}" ]]; then
			echo "[start] Refusing absolute Linux JENKINS_CONFIG_ROOT under WSL: $REQUESTED_JENKINS_CONFIG_ROOT" >&2
			echo "[start] Use ./jenkins_config so Docker Desktop mounts the real WSL project folder." >&2
			exit 1
		fi

		JENKINS_CONFIG_ROOT="$REQUESTED_JENKINS_CONFIG_ROOT"
		;;
	*)
		JENKINS_CONFIG_ROOT="$REQUESTED_JENKINS_CONFIG_ROOT"
		;;
esac

echo "[start] Using JENKINS_CONFIG_ROOT=$JENKINS_CONFIG_ROOT"
echo "[start] Using image ${DOCKER_HUB_USERNAME}/prod-eng-img:${IMAGE_TAG}"
mkdir -p "${SCRIPT_DIR}/jenkins_config"

ENV_FILE=".env"
printf 'JENKINS_CONFIG_ROOT=%s\nDOCKER_HUB_USERNAME=%s\nIMAGE_TAG=%s\n' \
	"$JENKINS_CONFIG_ROOT" \
	"$DOCKER_HUB_USERNAME" \
	"$IMAGE_TAG" > "$ENV_FILE"

docker compose --profile mongo --profile prod-eng-service up -d
