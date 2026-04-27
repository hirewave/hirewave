#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SRC_DIR="${JENKINS_BACKUP_SOURCE:-${SCRIPT_DIR}/jenkins_config}"
DEST_PARENT="${JENKINS_BACKUP_PARENT:-$(dirname "$SRC_DIR")}"
BACKUP_PREFIX="${JENKINS_BACKUP_PREFIX:-jenkins-save-backup-}"
INTERVAL_SECONDS="${JENKINS_BACKUP_INTERVAL:-30}"

PID_FILE="${SCRIPT_DIR}/.jenkins-backup-worker.pid"
LOG_FILE="${SCRIPT_DIR}/.jenkins-backup-worker.log"
WORKER_FILE="${SCRIPT_DIR}/.jenkins-backup-worker.py"

log() {
	printf '[backup-jenkins] %s\n' "$*"
}

usage() {
	cat <<EOF
Usage: $0 [start|stop|restart|status|logs]

Environment variables:
	JENKINS_BACKUP_SOURCE   Source Jenkins home (default: /home/traian/code/jenkins-save)
	JENKINS_BACKUP_PARENT   Destination parent dir (default: parent of source)
	JENKINS_BACKUP_PREFIX   Backup folder prefix (default: jenkins-save-backup-)
	JENKINS_BACKUP_INTERVAL Interval in seconds (default: 30)

Backup folders are created as:
	<JENKINS_BACKUP_PARENT>/<JENKINS_BACKUP_PREFIX><number>
EOF
}

worker_running() {
	if [[ -f "$PID_FILE" ]]; then
		local pid
		pid="$(cat "$PID_FILE")"
		if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
			return 0
		fi
	fi
	return 1
}

write_worker() {
	cat > "$WORKER_FILE" <<'PY'
#!/usr/bin/env python3
import os
import shutil
import signal
import sys
import time
from datetime import datetime
from pathlib import Path

SRC = Path(sys.argv[1]).resolve()
DEST_PARENT = Path(sys.argv[2]).resolve()
PREFIX = sys.argv[3]
INTERVAL = int(sys.argv[4])
LOG_PATH = Path(sys.argv[5]).resolve()

running = True


def log(msg: str) -> None:
		timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
		line = f"[{timestamp}] {msg}"
		with LOG_PATH.open("a", encoding="utf-8") as f:
				f.write(line + "\n")


def handle_signal(signum, _frame):
		global running
		running = False
		log(f"Received signal {signum}; stopping backup worker")


def next_backup_dir() -> Path:
		n = 1
		while True:
				candidate = DEST_PARENT / f"{PREFIX}{n}"
				if not candidate.exists():
						return candidate
				n += 1


def snapshot_once() -> None:
		if not SRC.exists():
				log(f"Source does not exist, skipping: {SRC}")
				return

		DEST_PARENT.mkdir(parents=True, exist_ok=True)

		final_dir = next_backup_dir()
		tmp_dir = DEST_PARENT / f".{PREFIX}tmp-{int(time.time())}"

		if tmp_dir.exists():
				shutil.rmtree(tmp_dir)

		# Copy into a temp directory first, then atomically rename.
		shutil.copytree(SRC, tmp_dir, symlinks=True)
		os.replace(tmp_dir, final_dir)
		log(f"Created backup: {final_dir}")


def main() -> int:
		signal.signal(signal.SIGTERM, handle_signal)
		signal.signal(signal.SIGINT, handle_signal)

		log(
				"Starting backup worker "
				f"(source={SRC}, dest_parent={DEST_PARENT}, prefix={PREFIX}, interval={INTERVAL}s)"
		)

		while running:
				try:
						snapshot_once()
				except Exception as exc:  # keep worker alive on copy issues
						log(f"Backup failed: {exc!r}")

				for _ in range(INTERVAL):
						if not running:
								break
						time.sleep(1)

		log("Backup worker stopped")
		return 0


if __name__ == "__main__":
		raise SystemExit(main())
PY

	chmod +x "$WORKER_FILE"
}

start_worker() {
	if ! command -v python3 >/dev/null 2>&1; then
		log "python3 is required but not installed"
		exit 1
	fi

	if [[ ! -d "$SRC_DIR" ]]; then
		log "Source Jenkins directory does not exist: $SRC_DIR"
		exit 1
	fi

	mkdir -p "$DEST_PARENT"
	write_worker

	if worker_running; then
		log "Worker already running with PID $(cat "$PID_FILE")"
		exit 0
	fi

	log "Starting worker in background"
	nohup python3 "$WORKER_FILE" "$SRC_DIR" "$DEST_PARENT" "$BACKUP_PREFIX" "$INTERVAL_SECONDS" "$LOG_FILE" >> "$LOG_FILE" 2>&1 &
	echo "$!" > "$PID_FILE"
	sleep 1

	if worker_running; then
		log "Worker started with PID $(cat "$PID_FILE")"
		log "Logs: $LOG_FILE"
	else
		log "Worker failed to start; check logs: $LOG_FILE"
		exit 1
	fi
}

stop_worker() {
	if ! worker_running; then
		log "Worker is not running"
		rm -f "$PID_FILE"
		exit 0
	fi

	local pid
	pid="$(cat "$PID_FILE")"
	log "Stopping worker PID $pid"
	kill "$pid" 2>/dev/null || true

	for _ in {1..20}; do
		if ! kill -0 "$pid" 2>/dev/null; then
			break
		fi
		sleep 0.5
	done

	rm -f "$PID_FILE"
	log "Worker stopped"
}

status_worker() {
	if worker_running; then
		log "Worker is running with PID $(cat "$PID_FILE")"
	else
		log "Worker is not running"
	fi

	log "Source: $SRC_DIR"
	log "Dest parent: $DEST_PARENT"
	log "Prefix: $BACKUP_PREFIX"
	log "Interval: ${INTERVAL_SECONDS}s"
	log "Log file: $LOG_FILE"
}

show_logs() {
	if [[ -f "$LOG_FILE" ]]; then
		tail -n 50 "$LOG_FILE"
	else
		log "No log file yet: $LOG_FILE"
	fi
}

command="${1:-start}"

case "$command" in
	start)
		start_worker
		;;
	stop)
		stop_worker
		;;
	restart)
		stop_worker || true
		start_worker
		;;
	status)
		status_worker
		;;
	logs)
		show_logs
		;;
	-h|--help|help)
		usage
		;;
	*)
		log "Unknown command: $command"
		usage
		exit 1
		;;
esac
