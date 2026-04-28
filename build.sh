#!/bin/bash -e

./gradlew clean test build

# Build the Docker image so changes take effect in containers
docker build -t prod-eng-img .