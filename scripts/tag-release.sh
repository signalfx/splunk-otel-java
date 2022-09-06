#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source "${SCRIPT_DIR}/common.sh"

ROOT_DIR="${SCRIPT_DIR}/../"
cd ${ROOT_DIR}

print_usage() {
  cat <<EOF
Usage: $(basename $0) release_version

All versions MUST NOT begin with 'v'. Example: 1.2.3.
EOF
}

if [[ $# < 1 ]]
then
  print_usage
  exit 1
fi

release_version=$1
release_tag="v$1"

validate_version "$release_version"

git tag -m "Release $release_tag" -s "$release_tag"
git push origin "$release_tag"
