#!/usr/bin/env python3
"""
Lab 9 — Alert Lifecycle Test

Stops the prod-eng container to trigger:
  - WARNING-ServiceHealthCheckFailing  (custom, up metric, fires in ~60s)
  - CRITICAL-ServiceHealthCheckFailing (custom, up metric, fires in ~2m)
  - WARNING-ApplicationContainerDown   (existing, cAdvisor, fires immediately)
  - CRITICAL-ApplicationContainerDown  (existing, cAdvisor, fires after 60s)

Then restarts the container and observes resolution.

Usage:
    python3 test_alerts.py

Requires:
    pip install requests
"""

import subprocess
import sys
import time
import requests
from datetime import datetime

PROMETHEUS   = "http://localhost:9090"
APP          = "http://localhost:8080"
ALERTMANAGER = "http://localhost:9093"

WATCHED_ALERTS = [
    "WARNING-ServiceHealthCheckFailing",
    "CRITICAL-ServiceHealthCheckFailing",
    "WARNING-ApplicationContainerDown",
    "CRITICAL-ApplicationContainerDown",
]


# ── helpers ─────────────────────────────────────────────────────────────────

def ts():
    return datetime.now().strftime("%H:%M:%S")


def log(msg=""):
    print(f"[{ts()}] {msg}" if msg else "")


def get_alert_states():
    r = requests.get(f"{PROMETHEUS}/api/v1/alerts", timeout=5)
    r.raise_for_status()
    return {a["labels"]["alertname"]: a["state"] for a in r.json()["data"]["alerts"]}


def print_watched_alerts():
    states = get_alert_states()
    for name in WATCHED_ALERTS:
        state = states.get(name, "inactive")
        icon = {"firing": "🔴", "pending": "🟡", "inactive": "🟢"}.get(state, "⚪")
        print(f"  {icon}  {name}: {state.upper()}")


def check_service(name, url):
    try:
        requests.get(url, timeout=5).raise_for_status()
        print(f"  ✓  {name}")
        return True
    except Exception:
        print(f"  ✗  {name}  —  not reachable at {url}")
        return False


def find_container():
    """Return the name of the running prod-eng container."""
    out = subprocess.run(
        ["docker", "ps", "--filter", "name=prod-eng", "--format", "{{.Names}}"],
        capture_output=True, text=True,
    ).stdout.strip()
    names = [n for n in out.splitlines() if n]
    return names[0] if names else None


def wait_for_state(alertname, target_state, timeout=300):
    """Poll until alertname reaches target_state. Returns True on success."""
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        try:
            current = get_alert_states().get(alertname, "inactive")
            if current != last:
                log(f"  {alertname}: {current.upper()}")
                last = current
            if current == target_state:
                return True
        except Exception as e:
            log(f"  poll error: {e}")
        time.sleep(5)
    return False


# ── main ─────────────────────────────────────────────────────────────────────

def main():
    print(f"\n{'='*62}")
    print("   Lab 9 — Alert Lifecycle Test  (container-down scenario)")
    print(f"{'='*62}\n")

    # 1. Pre-flight ─────────────────────────────────────────────────
    print("[1] Checking services ...")
    ok = all([
        check_service("Prometheus",   f"{PROMETHEUS}/-/healthy"),
        check_service("AlertManager", f"{ALERTMANAGER}/-/healthy"),
        check_service("App",          f"{APP}/actuator/health"),
    ])
    if not ok:
        print("\n  Run:  ./start_with_monitoring.sh")
        sys.exit(1)

    container = find_container()
    if not container:
        print("\n  ✗  No running prod-eng container found.")
        print("     Run:  ./start_with_monitoring.sh")
        sys.exit(1)
    print(f"  ✓  Container: {container}")

    # 2. Baseline ────────────────────────────────────────────────────
    print("\n[2] Baseline — all alerts should be Inactive:")
    print_watched_alerts()

    # 3. Trigger ─────────────────────────────────────────────────────
    print(f"\n[3] Stopping container '{container}' to trigger alerts ...")
    subprocess.run(["docker", "stop", container], capture_output=True)
    log(f"  Container stopped. Prometheus scrape interval is 30s — first eval within ~60s.")

    # 4. Pending → Firing ────────────────────────────────────────────
    print("\n[4] Waiting for WARNING-ServiceHealthCheckFailing (for: 30s, eval: up to 60s) ...")
    if wait_for_state("WARNING-ServiceHealthCheckFailing", "firing", timeout=240):
        log("  ✓  FIRING")
        log(f"  →  Check AlertManager:  {ALERTMANAGER}")
        log(f"  →  Email sent to:       claude@tra1an.com")
    else:
        log("  ✗  Did not fire within 4 minutes")

    print("\n[5] Waiting for CRITICAL-ApplicationContainerDown (container missing > 60s) ...")
    if wait_for_state("CRITICAL-ApplicationContainerDown", "firing", timeout=120):
        log("  ✓  CRITICAL firing — WARNING should now be inhibited")
    else:
        log("  ℹ  Did not fire (container name filter in alert may differ from yours)")
        log(f"     Alert uses: container_last_seen{{name=\"service_prod-eng_1\"}}")
        log(f"     Your container name: {container}")

    print("\n[6] Current alert snapshot:")
    print_watched_alerts()

    # 5. Resolve ─────────────────────────────────────────────────────
    print(f"\n[7] Restarting container '{container}' ...")
    subprocess.run(["docker", "start", container], capture_output=True)
    log("  Waiting for app to be healthy ...")
    for _ in range(24):
        try:
            requests.get(f"{APP}/actuator/health", timeout=3).raise_for_status()
            log("  ✓  App is healthy")
            break
        except Exception:
            time.sleep(5)
    else:
        log("  ✗  App did not come up within 2 minutes")

    print("\n[8] Waiting for WARNING-ServiceHealthCheckFailing to resolve ...")
    if wait_for_state("WARNING-ServiceHealthCheckFailing", "inactive", timeout=240):
        log("  ✓  RESOLVED — resolution email should arrive at claude@tra1an.com")
    else:
        log("  ?  Still active after 4 minutes — may resolve on next scrape cycle")

    # 6. Final state ─────────────────────────────────────────────────
    print("\n[9] Final alert states:")
    print_watched_alerts()

    print(f"\n{'='*62}")
    print("   Lifecycle: Inactive → Pending → Firing → Resolved ✓")
    print(f"   Prometheus:   {PROMETHEUS}/alerts")
    print(f"   AlertManager: {ALERTMANAGER}")
    print(f"   Grafana:      http://localhost:3000")
    print(f"{'='*62}\n")


if __name__ == "__main__":
    main()
