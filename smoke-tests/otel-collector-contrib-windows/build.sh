#!/bin/bash

EXTRA_TAG=$1
IMAGE_NAME="ghcr.io/signalfx/splunk-otel-java/otel-collector-contrib-windows:$(date '+%Y%m%d').${EXTRA_TAG}"

echo "Building image ${IMAGE_NAME} ..."

docker build -t "$IMAGE_NAME" .
docker push "$IMAGE_NAME"
