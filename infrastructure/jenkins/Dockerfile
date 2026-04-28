FROM jenkins/jenkins

USER root
ARG DOCKER_COMPOSE_VERSION=v2.29.7
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl docker.io docker-cli \
    && mkdir -p /usr/lib/docker/cli-plugins \
    && curl -fsSL "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64" -o /usr/lib/docker/cli-plugins/docker-compose \
    && chmod +x /usr/lib/docker/cli-plugins/docker-compose \
    && rm -rf /var/lib/apt/lists/*

RUN /opt/java/openjdk/bin/java -jar /opt/jenkins-plugin-manager.jar --plugins htmlpublisher

# Keep root to match existing docker socket usage in compose (user: 0:0).
USER root
