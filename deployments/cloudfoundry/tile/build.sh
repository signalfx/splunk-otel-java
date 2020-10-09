#!/bin/bash

set -euo pipefail

if [[ $# -lt 1 ]]
then
    cat <<EOF
    Usage: ${0} TILE_VERSION
    This script requires a single parameter: new version of the tile.
EOF
    exit 1
fi

TILE_VERSION="$1"
TILE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILDPACK_RELEASE="${TILE_DIR}/resources/splunk_otel_java_buildpack-linux.zip"

# clean old release
rm -rf release product
# tile history only causes problems during development
rm -f tile-history.yml
# clean old buildpacks
rm -f resources/*.zip

# build the buildpack
(cd "${TILE_DIR}/../buildpack" && \
    echo "$TILE_VERSION" > VERSION && \
    zip "$BUILDPACK_RELEASE" bin/* manifest.yml README.md VERSION)

# build the tile
tile build "$TILE_VERSION"
