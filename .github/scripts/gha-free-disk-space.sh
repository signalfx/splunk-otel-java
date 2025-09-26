#!/bin/bash -e

# Copied from https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/.github/scripts/gha-free-disk-space.sh

df -h
sudo rm -rf /usr/local/lib/android
sudo rm -rf /usr/share/dotnet
df -h
