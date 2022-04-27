#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
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
# without the starting 'v'
release_version=$(echo "$release_tag" | cut -c2-)
# 1 from 1.2.3
major_version=$(echo "$release_version" | awk -F'.' '{print $1}')
minor_version=$(echo "$release_version" | awk -F'.' '{print $2}')
patch_version=$(echo "$release_version" | awk -F'.' '{print $3}')

setup_gpg() {
  echo ">>> Setting GnuPG configuration ..."
  mkdir -p ~/.gnupg
  chmod 700 ~/.gnupg
  cat > ~/.gnupg/gpg.conf <<EOF
no-tty
pinentry-mode loopback
EOF
}

import_gpg_secret_key() {
  local secret_key_contents="$1"

  echo ">>> Importing secret key ..."
  echo "$secret_key_contents" > seckey.gpg
  if (gpg --batch --allow-secret-key-import --import seckey.gpg)
  then
    rm seckey.gpg
  else
    rm seckey.gpg
    exit 1
  fi
}

build_project() {
  mkdir -p dist

  echo ">>> Building the javaagent ..."
  ./gradlew -Prelease.useLastTag=true build final closeAndReleaseSonatypeStagingRepository -x test --no-daemon
  mv agent/build/libs/splunk-otel-javaagent-${release_version}.jar dist/splunk-otel-javaagent.jar
  mv agent/build/libs/splunk-otel-javaagent-${release_version}.jar.asc dist/splunk-otel-javaagent.jar.asc
  mv agent/build/libs/splunk-otel-javaagent-${release_version}-all.jar dist/splunk-otel-javaagent-all.jar
  mv agent/build/libs/splunk-otel-javaagent-${release_version}-all.jar.asc dist/splunk-otel-javaagent-all.jar.asc

  echo ">>> Building the cloudfoundry buildpack ..."
  ./deployments/cloudfoundry/buildpack/build.sh
  mv deployments/cloudfoundry/buildpack/splunk_otel_java_buildpack-linux.zip dist/
  sign_file dist/splunk_otel_java_buildpack-linux.zip

  echo ">>> Calculating checksums ..."
  shasum -a 256 dist/* > dist/checksums.txt
  sign_file dist/checksums.txt
}

sign_file() {
  local file="$1"
  echo "$GPG_PASSWORD" | \
    gpg --batch --passphrase-fd 0 --armor --default-key="$GPG_KEY_ID" --detach-sign "$file"
}

create_gh_release() {
  echo ">>> Creating GitHub release $release_tag ..."
  gh release create "$release_tag" dist/* \
    --repo "signalfx/splunk-otel-java" \
    --draft \
    --title "Release $release_tag"
}

build_docker_image() {
  echo ">>> Building the operator docker image ..."
  docker build -t splunk-otel-instrumentation-java .
  docker tag splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag
  docker tag splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:v$major_version
  docker tag splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:latest
}

login_to_quay_io() {
  echo ">>> Logging into quay.io ..."
  docker login -u $QUAY_USERNAME -p $QUAY_PASSWORD quay.io
}

publish_docker_image() {
  echo ">>> Publishing the operator docker image ..."
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:latest
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:v$major_version
}

setup_git() {
  git config --global user.name release-bot
  git config --global user.email ssg-srv-gh-o11y-gdi@splunk.com
  git config --global gpg.program gpg
  git config --global user.signingKey "$GITHUB_BOT_GPG_KEY_ID"
}

create_post_release_pr() {
  if [[ $patch_version != 0 ]]
  then
    echo ">>> Patch release detected, skipping the post-release.sh script ..."
    return
  fi

  local repo="signalfx/splunk-otel-java"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local next_version="${major_version}.$((minor_version + 1)).0"
  local post_release_branch="post-release-changes-$release_tag"

  echo ">>> Cloning the splunk-otel-java repository ..."
  git clone "$repo_url" javaagent-mirror

  echo ">>> Applying the post-release.sh script and pushing changes ..."
  cd javaagent-mirror
  git checkout -b "$post_release_branch"
  ./scripts/post-release.sh "$release_version" "$next_version"
  git commit -S -am "$release_tag post release changes"
  git push "$repo_url" "$post_release_branch"

  echo ">>> Creating the $release_tag post release PR ..."
  local message="[automated] $release_tag post release changes"
  gh pr create \
    --repo "$repo" \
    --title "$message" \
    --body "$message" \
    --base main \
    --head "$post_release_branch"
}

create_overhead_test_pr() {
  local repo="signalfx/splunk-otel-java-overhead-test"
  local repo_url="https://srv-gh-o11y-gdi:${GITHUB_TOKEN}@github.com/${repo}.git"
  local update_version_branch="update-agent-version-$release_tag"

  echo ">>> Cloning the splunk-otel-java-overhead-test repository ..."
  git clone "$repo_url" overhead-test-mirror

  echo ">>> Applying the update-version.sh script and pushing changes ..."
  cd overhead-test-mirror
  git checkout -b "$update_version_branch"
  ./scripts/update-version.sh "$release_version"
  git commit -S -am "Update agent version to $release_tag"
  git push "$repo_url" "$update_version_branch"

  echo ">>> Creating the agent version update PR ..."
  local message="[automated] Update agent version to $release_tag"
  gh pr create \
    --repo "$repo" \
    --title "$message" \
    --body "$message" \
    --base main \
    --head "$update_version_branch"
}

setup_gpg
import_gpg_secret_key "$GPG_SECRET_KEY"
build_project
create_gh_release

build_docker_image
login_to_quay_io
publish_docker_image

import_gpg_secret_key "$GITHUB_BOT_GPG_KEY"
setup_git
create_post_release_pr
create_overhead_test_pr
