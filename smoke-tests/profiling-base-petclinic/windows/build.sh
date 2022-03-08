#!/bin/bash

IMAGE_NAME="ghcr.io/signalfx/splunk-otel-java/profiling-petclinic-base-windows-jdk$1:latest"

echo "Building image ${IMAGE_NAME} ..."

docker build --build-arg jdkVersion=$1 -t "$IMAGE_NAME" .
docker push "$IMAGE_NAME"
