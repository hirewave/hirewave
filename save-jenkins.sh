#!/usr/bin/env bash
set -euo pipefail

DEFAULT_SOURCE_JENKINS_HOME="/workspaces/jenkins_config"
TARGET_JENKINS_HOME="/home/traian/code/jenkins-save"

log() {
  printf '[save-jenkins] %s\n' "$*"
}

detect_active_jenkins_home() {
  docker inspect hirewave-jenkins-1 --format '{{range .Mounts}}{{println .Source "->" .Destination}}{{end}}' 2>/dev/null \
    | awk -F'->' '$2 ~ /\/var\/jenkins_home/ {gsub(/^[ \t]+|[ \t]+$/, "", $1); print $1; exit}'
}

sync_with_docker_fallback() {
  local src="$1"
  local dst="$2"

  log "Retrying sync using Docker helper container to bypass host file permission restrictions"
  echo "CMD: docker run --rm -v $src:/src:ro -v $dst:/dst busybox sh -lc cp -a /src/. /dst/"
  docker run --rm \
    -v "$src":/src:ro \
    -v "$dst":/dst \
    busybox \
    sh -lc 'cp -a /src/. /dst/'
}

has_jenkins_markers() {
  local dir="$1"
  [[ -f "$dir/config.xml" ]] &&
    [[ -d "$dir/jobs" ]] &&
    [[ -d "$dir/plugins" ]] &&
    [[ -d "$dir/secrets" ]] &&
    [[ -d "$dir/users" ]]
}

SOURCE_JENKINS_HOME="${SOURCE_JENKINS_HOME:-$DEFAULT_SOURCE_JENKINS_HOME}"

ACTIVE_JENKINS_HOME="$(detect_active_jenkins_home || true)"
if [[ -n "$ACTIVE_JENKINS_HOME" ]]; then
  log "Detected active Jenkins mount source: $ACTIVE_JENKINS_HOME"
  SOURCE_JENKINS_HOME="$ACTIVE_JENKINS_HOME"
else
  log "Could not detect active Jenkins mount; using default source: $SOURCE_JENKINS_HOME"
fi

if [[ "$SOURCE_JENKINS_HOME" == "$TARGET_JENKINS_HOME" ]]; then
  log "Jenkins is already using persistent path ($TARGET_JENKINS_HOME); nothing to sync"
  exit 0
fi

if [[ ! -d "$SOURCE_JENKINS_HOME" ]]; then
  log "Source Jenkins home does not exist: $SOURCE_JENKINS_HOME"
  exit 1
fi

if ! has_jenkins_markers "$SOURCE_JENKINS_HOME"; then
  log "Refusing to sync: source does not look like a valid Jenkins home"
  exit 1
fi

log "Ensuring destination exists: $TARGET_JENKINS_HOME"
mkdir -p "$TARGET_JENKINS_HOME"

if command -v rsync >/dev/null 2>&1; then
  log "Syncing with rsync -a (non-destructive, no --delete)"
  echo "CMD: rsync -a $SOURCE_JENKINS_HOME/ $TARGET_JENKINS_HOME/"
  if ! rsync -a "$SOURCE_JENKINS_HOME/" "$TARGET_JENKINS_HOME/"; then
    log "rsync failed; attempting Docker fallback"
    sync_with_docker_fallback "$SOURCE_JENKINS_HOME" "$TARGET_JENKINS_HOME"
  fi
else
  log "rsync not found; using cp -a fallback (non-destructive)"
  echo "CMD: cp -a $SOURCE_JENKINS_HOME/. $TARGET_JENKINS_HOME/"
  if ! cp -a "$SOURCE_JENKINS_HOME/." "$TARGET_JENKINS_HOME/"; then
    log "cp -a failed; attempting Docker fallback"
    sync_with_docker_fallback "$SOURCE_JENKINS_HOME" "$TARGET_JENKINS_HOME"
  fi
fi

log "Sync completed"