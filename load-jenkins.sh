#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_JENKINS_HOME="/workspaces/jenkins_config"
TARGET_JENKINS_HOME="/home/traian/code/jenkins-save"

log() {
  printf '[load-jenkins] %s\n' "$*"
}

has_jenkins_markers() {
  local dir="$1"
  [[ -f "$dir/config.xml" ]] &&
    [[ -d "$dir/jobs" ]] &&
    [[ -d "$dir/plugins" ]] &&
    [[ -d "$dir/secrets" ]] &&
    [[ -d "$dir/users" ]]
}

sync_dir_non_destructive() {
  local src="$1"
  local dst="$2"

  if command -v rsync >/dev/null 2>&1; then
    log "Syncing with rsync -a from $src to $dst"
    rsync -a "$src/" "$dst/"
  else
    log "rsync not found; syncing with cp -a from $src to $dst"
    cp -a "$src/." "$dst/"
  fi
}

log "Ensuring persistent Jenkins directory exists: $TARGET_JENKINS_HOME"
mkdir -p "$TARGET_JENKINS_HOME"

source_valid=false
target_valid=false

if has_jenkins_markers "$SOURCE_JENKINS_HOME"; then
  source_valid=true
  log "Source Jenkins home looks valid: $SOURCE_JENKINS_HOME"
else
  log "Source Jenkins home is missing one or more markers: $SOURCE_JENKINS_HOME"
fi

if has_jenkins_markers "$TARGET_JENKINS_HOME"; then
  target_valid=true
  log "Target Jenkins home already looks valid and will be preferred: $TARGET_JENKINS_HOME"
else
  log "Target Jenkins home is empty/incomplete: $TARGET_JENKINS_HOME"
fi

if [[ "$target_valid" == false && "$source_valid" == true ]]; then
  log "Populating target from source because target is empty/incomplete and source is valid"
  sync_dir_non_destructive "$SOURCE_JENKINS_HOME" "$TARGET_JENKINS_HOME"
fi

if [[ "$target_valid" == false ]] && has_jenkins_markers "$TARGET_JENKINS_HOME"; then
  target_valid=true
  log "Target Jenkins home became valid after sync"
fi

log "Stopping running compose stack before restart"
(
  cd "$REPO_DIR"
  docker compose --profile mongo --profile prod-eng-service down || true
)

log "Starting stack with persistent Jenkins root"
(
  cd "$REPO_DIR"
  JENKINS_CONFIG_ROOT="$TARGET_JENKINS_HOME" ./start.sh
)

log "Verifying Jenkins mount"
echo "CMD: docker inspect hirewave-jenkins-1 --format '{{range .Mounts}}{{println .Source \"->\" .Destination}}{{end}}'"
docker inspect hirewave-jenkins-1 --format '{{range .Mounts}}{{println .Source "->" .Destination}}{{end}}'

if [[ "$target_valid" == false ]]; then
  log "WARNING: Target still does not have full marker set. Jenkins will still use $TARGET_JENKINS_HOME, but state may be minimal."
fi

log "Done"