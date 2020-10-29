#!/bin/bash

set -euo pipefail

BUILDPACK_RELEASE="splunk_otel_java_buildpack-linux.zip"

zip "$BUILDPACK_RELEASE" bin/* manifest.yml README.md
