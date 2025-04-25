#!/usr/bin/env bash
set -e

# NOTE: this script is meant to be run on the GitLab CI, it depends on GitLab CI variables

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

build_docker_image() {
  echo ">>> Building the operator docker image ..."
  docker build -t splunk-otel-instrumentation-java .
  docker tag splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:latest
  docker tag splunk-otel-instrumentation-java "quay.io/signalfx/splunk-otel-instrumentation-java:v$(get_major_version "$release_tag")"
  docker tag splunk-otel-instrumentation-java "quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag"
}

login_to_quay_io() {
  echo ">>> Logging into quay.io ..."
  docker login -u "$QUAY_USERNAME" -p "$QUAY_PASSWORD" quay.io
}

publish_docker_image() {
  echo ">>> Publishing the operator docker image ..."
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:latest
  docker push "quay.io/signalfx/splunk-otel-instrumentation-java:v$(get_major_version "$release_tag")"
  docker push "quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag"
}

sign_published_docker_image() {
  echo ">>> Signing the published operator docker image ..."
  artifact-ci sign docker "quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag"
}

build_docker_image
login_to_quay_io
publish_docker_image
sign_published_docker_image
