FROM jenkins/jenkins

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io docker-cli \
    && rm -rf /var/lib/apt/lists/*

# Keep root to match existing docker socket usage in compose (user: 0:0).
USER root
