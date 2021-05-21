#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

BUILDPACK_RELEASE="splunk_otel_java_buildpack-linux.zip"

zip "$BUILDPACK_RELEASE" bin/* manifest.yml README.md
