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

create_post_release_pr() {
  if [[ "$(get_patch_version "$release_tag")" != 0 ]]
  then
    echo ">>> Patch release detected, skipping the post-release.sh script ..."
    return
  fi

  local repo="signalfx/splunk-otel-java"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local post_release_branch="post-release-changes-$release_tag"
  local message="$release_tag post release changes"
  local next_version
  next_version="$(get_major_version "$release_tag").$(($(get_minor_version "$release_tag") + 1)).0"

  echo ">>> Cloning the $repo repository ..."
  git clone "$repo_url" javaagent-mirror

  echo ">>> Applying the post-release.sh script and pushing changes ..."
  cd javaagent-mirror
  git checkout -b "$post_release_branch"
  ./scripts/update-version-after-release.sh "$(get_release_version "$release_tag")" "$next_version"
  git commit -S -am "[automated] $message"
  git push "$repo_url" "$post_release_branch"

  echo ">>> Creating the $release_tag post release PR ..."
  gh pr create \
    --repo "$repo" \
    --title "$message" \
    --body "$message" \
    --label automated \
    --base main \
    --head "$post_release_branch"
}

create_collector_helm_chart_pr() {
  local repo="signalfx/splunk-otel-collector-chart"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local update_version_branch="java-version-update-$release_tag"
  local message="[java-version-update] Update agent version to $release_tag"
  local java_repo = "ghcr.io/signalfx/splunk-otel-java/splunk-otel-java"

  echo ">>> Cloning the $repo repository ..."
  git clone "$repo_url" collector-chart-mirror

  cd collector-chart-mirror
  git checkout -b "$update_version_branch"

  # This relies on the tag being on the line after the java repo
  echo ">>> Updating versions inline: values.yaml"
  VLINE=$(grep -n -A 1 $java_repo helm-charts/splunk-otel-collector/values.yaml | tail -1 | sed -e "s/-.*//")
  sed -i "${VLINE}s/\"v.*\"/\"${release_tag}\"/" helm-charts/splunk-otel-collector/values.yaml

  # Update the examples as well
  echo ">>> Updating versions inline: deployment.yaml"
  sed -i "s#${java_repo}:v.*#${java_repo}:${release_tag}#" \
    examples/enable-operator-and-auto-instrumentation/rendered_manifests/operator/deployment.yaml

  git commit -S -am "[automated] $message"
  git push "$repo_url" "$update_version_branch"

  echo ">>> Creating a PR in splunk-otel-collector-chart..."
  gh pr create \
    --repo "$repo" \
    --title "$message" \
    --body "$message" \
    --label automated \
    --base main \
    --head "$update_version_branch"
}

create_collector_pr() {
  local repo="signalfx/splunk-otel-collector"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local update_version_branch="javaagent-version-update-$release_tag"
  local message="[javaagent-version-update] Update javaagent version to $release_tag"

  echo ">>> Cloning the $repo repository ..."
  git clone "$repo_url" collector-mirror

  echo ">>> Updating the version and pushing changes ..."
  cd collector-mirror
  git checkout -b "$update_version_branch"
  echo "$release_tag" > instrumentation/packaging/java-agent-release.txt
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

setup_gpg
import_gpg_secret_key "$GITHUB_BOT_GPG_KEY"
setup_git
create_post_release_pr
create_collector_pr
create_collector_helm_chart_pr
