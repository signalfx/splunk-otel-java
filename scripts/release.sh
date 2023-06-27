#!/usr/bin/env bash

# NOTE: this script is meant to be run on the GitLab CI, it depends on GitLab CI variables

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source-path=SCRIPTDIR
source "${SCRIPT_DIR}/common.sh"

ROOT_DIR="${SCRIPT_DIR}/../"
cd "${ROOT_DIR}"

print_usage() {
  cat <<EOF
Usage: $(basename "$0") tag"

Tag example: v1.2.3
EOF
}

if [[ $# != 1 ]]
then
  print_usage
  exit 1
fi

release_tag="$1"

validate_project_version() {
  if (grep SNAPSHOT version.gradle.kts >/dev/null)
  then
    echo "Cannot release a SNAPSHOT version!"
    echo "Did you run the scripts/pre-release-changes.sh script before running the release process?"
    exit 1
  fi
}

build_project() {
  local release_version
  release_version="$(get_release_version "$release_tag")"

  mkdir -p dist

  echo ">>> Building the javaagent ..."
  ./gradlew assemble publishToSonatype closeAndReleaseSonatypeStagingRepository --no-daemon --stacktrace
  mv "agent/build/libs/splunk-otel-javaagent-${release_version}.jar" dist/splunk-otel-javaagent.jar
  mv "agent/build/libs/splunk-otel-javaagent-${release_version}.jar.asc" dist/splunk-otel-javaagent.jar.asc
  mv "agent/build/libs/splunk-otel-javaagent-${release_version}-all.jar" dist/splunk-otel-javaagent-all.jar
  mv "agent/build/libs/splunk-otel-javaagent-${release_version}-all.jar.asc" dist/splunk-otel-javaagent-all.jar.asc

  echo ">>> Building the cloudfoundry buildpack ..."
  ./deployments/cloudfoundry/buildpack/build.sh
  mv deployments/cloudfoundry/buildpack/splunk_otel_java_buildpack-linux.zip dist/
  sign_file dist/splunk_otel_java_buildpack-linux.zip

  echo ">>> Calculating checksums ..."
  shasum -a 256 dist/* > dist/checksums.txt
  sign_file dist/checksums.txt
}

create_gh_release() {
  echo ">>> Creating GitHub release $release_tag ..."
  gh release create "$release_tag" dist/* \
    --repo "signalfx/splunk-otel-java" \
    --draft \
    --title "Release $release_tag"
}

validate_project_version
setup_gpg
import_gpg_secret_key "$GPG_SECRET_KEY"
build_project
create_gh_release
