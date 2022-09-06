#!/usr/bin/env bash
set -e

# NOTE: this script is meant to be run on the GitLab CI, it depends on GitLab CI variables

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source "${SCRIPT_DIR}/common.sh"

ROOT_DIR="${SCRIPT_DIR}/../"
cd ${ROOT_DIR}

print_usage() {
  cat <<EOF
Usage: $(basename $0) tag"

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
  docker tag "splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:v$(get_major_version $release_tag)"
  docker tag "splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag"
}

login_to_quay_io() {
  echo ">>> Logging into quay.io ..."
  docker login -u $QUAY_USERNAME -p $QUAY_PASSWORD quay.io
}

publish_docker_image() {
  echo ">>> Publishing the operator docker image ..."
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:latest
  docker push "quay.io/signalfx/splunk-otel-instrumentation-java:v$(get_major_version $release_tag)"
  docker push "quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag"
}

create_operator_pr() {
  local repo="signalfx/splunk-otel-collector-operator"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local update_version_branch="javaagent-version-update-$release_tag"
  local message="[javaagent-version-update] Update javaagent version to $release_tag"

  echo ">>> Cloning the $repo repository ..."
  git clone "$repo_url" operator-mirror

  echo ">>> Updating the version and pushing changes ..."
  cd operator-mirror
  git checkout -b "$update_version_branch"
  echo "$release_tag" > instrumentation/packaging/java-agent-release.txt
  ./.ci/update-javaagent-version.sh "$(get_release_version $release_tag)"
  git commit -S -am "[automated] $message"
  git push "$repo_url" "$update_version_branch"

  echo ">>> Creating the agent version update PR ..."
  gh pr create \
    --repo "$repo" \
    --title "$message" \
    --body "$message" \
    --base main \
    --head "$update_version_branch"
}

build_docker_image
login_to_quay_io
publish_docker_image

setup_gpg
import_gpg_secret_key "$GITHUB_BOT_GPG_KEY"
setup_git
create_operator_pr
