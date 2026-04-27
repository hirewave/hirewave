#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_NAME="hirewave-jenkins-1"
JENKINS_HOME_HOST="${SCRIPT_DIR}/jenkins_config"
PASSWORD_FILE_CONTAINER="/var/jenkins_home/secrets/initialAdminPassword"
PASSWORD_FILE_HOST="$JENKINS_HOME_HOST/secrets/initialAdminPassword"
RECOVERED_ADMIN_PASSWORD_FILE="${SCRIPT_DIR}/.jenkins-admin-password"

log() {
  printf '[show-jenkins-pwd] %s\n' "$*"
}

if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  if docker exec "$CONTAINER_NAME" test -f "$PASSWORD_FILE_CONTAINER"; then
    log "Reading initial setup password from running container: $CONTAINER_NAME"
    docker exec "$CONTAINER_NAME" cat "$PASSWORD_FILE_CONTAINER"
    exit 0
  fi

  log "Jenkins is running, but the initial setup password file no longer exists."
  if [[ -f "$RECOVERED_ADMIN_PASSWORD_FILE" ]]; then
    log "Showing recovered local admin login:"
    cat "$RECOVERED_ADMIN_PASSWORD_FILE"
    exit 0
  fi

  log "That usually means Jenkins was already configured and the initial unlock password is no longer available."
  log "Use the admin user/password you configured in Jenkins."
  exit 1
fi

if [[ -f "$PASSWORD_FILE_HOST" ]]; then
  log "Container is not running; reading initial setup password from host path: $PASSWORD_FILE_HOST"
  cat "$PASSWORD_FILE_HOST"
  exit 0
fi

if [[ -f "$RECOVERED_ADMIN_PASSWORD_FILE" ]]; then
  log "Initial setup password is unavailable; showing recovered local admin login:"
  cat "$RECOVERED_ADMIN_PASSWORD_FILE"
  exit 0
fi

log "Could not find the Jenkins initial setup password."
log "If Jenkins has already been configured, this is expected."
log "Use the admin user/password you configured in Jenkins."
log "Tried:"
log "  - running container: $CONTAINER_NAME"
log "  - host file: $PASSWORD_FILE_HOST"
exit 1
