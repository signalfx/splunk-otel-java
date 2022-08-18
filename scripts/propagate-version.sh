#!/usr/bin/env bash
set -e

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

create_post_release_pr() {
  if [[ $patch_version != 0 ]]
  then
    echo ">>> Patch release detected, skipping the post-release.sh script ..."
    return
  fi

  local repo="signalfx/splunk-otel-java"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local next_version="$(get_major_version $release_tag).$(($(get_minor_version $release_tag) + 1)).0"
  local post_release_branch="post-release-changes-$release_tag"
  local message="$release_tag post release changes"

  echo ">>> Cloning the $repo repository ..."
  git clone "$repo_url" javaagent-mirror

  echo ">>> Applying the post-release.sh script and pushing changes ..."
  cd javaagent-mirror
  git checkout -b "$post_release_branch"
  ./scripts/update-version-after-release.sh "$(get_release_version $release_tag)" "$next_version"
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

create_overhead_test_pr() {
  local repo="signalfx/splunk-otel-java-overhead-test"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local update_version_branch="agent-version-update-$release_tag"
  local message="[agent-version-update] Update agent version to $release_tag"

  echo ">>> Cloning the $repo repository ..."
  git clone "$repo_url" overhead-test-mirror

  echo ">>> Applying the update-version.sh script and pushing changes ..."
  cd overhead-test-mirror
  git checkout -b "$update_version_branch"
  ./scripts/update-version.sh "$(get_release_version $release_tag)"
  git commit -S -am "[automated] $message"
  git push "$repo_url" "$update_version_branch"

  echo ">>> Creating the agent version update PR ..."
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
create_overhead_test_pr
create_collector_pr
