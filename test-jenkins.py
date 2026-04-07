#!/usr/bin/env python3
import os
import subprocess
import sys

PERSISTENT_HOME = "/home/traian/code/jenkins-save"
MARKERS = {
    "config.xml": "file",
    "jobs": "dir",
    "plugins": "dir",
    "secrets": "dir",
    "users": "dir",
}


def normalize_source(path: str) -> str:
    return path.strip().replace("\\", "/").rstrip("/")


def main() -> int:
    failures = []

    print("=== Jenkins Persistence Test ===")
    print(f"Checking persistent Jenkins home: {PERSISTENT_HOME}")

    if not os.path.isdir(PERSISTENT_HOME):
        failures.append(f"Missing directory: {PERSISTENT_HOME}")
    else:
        for marker, marker_type in MARKERS.items():
            marker_path = os.path.join(PERSISTENT_HOME, marker)
            if marker_type == "file" and not os.path.isfile(marker_path):
                failures.append(f"Missing file marker: {marker_path}")
            if marker_type == "dir" and not os.path.isdir(marker_path):
                failures.append(f"Missing directory marker: {marker_path}")

    inspect_cmd = [
        "docker",
        "inspect",
        "hirewave-jenkins-1",
        "--format",
        "{{range .Mounts}}{{println .Source \"->\" .Destination}}{{end}}",
    ]
    print("Running:", " ".join(inspect_cmd))

    inspect_result = subprocess.run(inspect_cmd, capture_output=True, text=True)
    if inspect_result.returncode != 0:
        failures.append(
            "docker inspect failed: "
            + (inspect_result.stderr.strip() or inspect_result.stdout.strip())
        )
    else:
        mount_output = inspect_result.stdout.strip()
        print("Mounts:")
        print(mount_output if mount_output else "(no mounts output)")

        expected_mount_found = False
        for line in mount_output.splitlines():
            if "->" not in line:
                continue
            source, destination = [part.strip() for part in line.split("->", 1)]
            source_norm = normalize_source(source)

            if destination == "/var/jenkins_home" and source_norm.endswith(
                "/home/traian/code/jenkins-save"
            ):
                expected_mount_found = True
                break

        if not expected_mount_found:
            failures.append(
                "Expected /home/traian/code/jenkins-save mounted to /var/jenkins_home was not found"
            )

    if failures:
        print("FAIL")
        for item in failures:
            print(f"- {item}")
        return 1

    print("PASS")
    print("- Persistent Jenkins directory exists")
    print("- Jenkins marker files/directories are present")
    print("- Jenkins container mount points to /home/traian/code/jenkins-save")
    return 0


if __name__ == "__main__":
    sys.exit(main())