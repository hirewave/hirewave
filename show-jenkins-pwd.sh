#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="hirewave-jenkins-1"
JENKINS_HOME_HOST="/home/traian/code/jenkins-save"
PASSWORD_FILE_CONTAINER="/var/jenkins_home/secrets/initialAdminPassword"
PASSWORD_FILE_HOST="$JENKINS_HOME_HOST/secrets/initialAdminPassword"

log() {
  printf '[show-jenkins-pwd] %s\n' "$*"
}

if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  log "Reading password from running container: $CONTAINER_NAME"
  docker exec "$CONTAINER_NAME" cat "$PASSWORD_FILE_CONTAINER"
  exit 0
fi

if [[ -f "$PASSWORD_FILE_HOST" ]]; then
  log "Container is not running; reading password from host path: $PASSWORD_FILE_HOST"
  cat "$PASSWORD_FILE_HOST"
  exit 0
fi

log "Could not find Jenkins password."
log "Tried:"
log "  - running container: $CONTAINER_NAME"
log "  - host file: $PASSWORD_FILE_HOST"
exit 1