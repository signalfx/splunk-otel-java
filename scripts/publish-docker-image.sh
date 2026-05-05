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
  local image_name="$1"
  local jar_file="$2"

  echo ">>> Building operator docker image ${image_name} ..."
  docker build --build-arg "JAR_FILE=${jar_file}" -t "${image_name}" .
  docker tag "${image_name}" "quay.io/signalfx/${image_name}:latest"
  docker tag "${image_name}" "quay.io/signalfx/${image_name}:v$(get_major_version "$release_tag")"
  docker tag "${image_name}" "quay.io/signalfx/${image_name}:$release_tag"
}

login_to_quay_io() {
  echo ">>> Logging into quay.io ..."
  docker login -u "$QUAY_USERNAME" -p "$QUAY_PASSWORD" quay.io
}

publish_docker_image() {
  local image_name="$1"

  echo ">>> Publishing the operator docker image ${image_name} ..."
  docker push "quay.io/signalfx/${image_name}:latest"
  docker push "quay.io/signalfx/${image_name}:v$(get_major_version "$release_tag")"
  docker push "quay.io/signalfx/${image_name}:$release_tag"
}

sign_published_docker_image() {
  local image_name="$1"

  echo ">>> Signing the published operator docker image ${image_name} ..."
  artifact-ci sign docker "quay.io/signalfx/${image_name}:$release_tag"
}

login_to_quay_io

build_docker_image splunk-otel-instrumentation-java splunk-otel-javaagent.jar
publish_docker_image splunk-otel-instrumentation-java
sign_published_docker_image splunk-otel-instrumentation-java

build_docker_image splunk-otel-instrumentation-java-csa splunk-otel-javaagent-csa.jar
publish_docker_image splunk-otel-instrumentation-java-csa
sign_published_docker_image splunk-otel-instrumentation-java-csa
