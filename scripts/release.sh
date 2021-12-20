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

import_gpg_keys() {
  echo ">>> Setting GnuPG configuration ..."
  mkdir -p ~/.gnupg
  chmod 700 ~/.gnupg
  cat > ~/.gnupg/gpg.conf <<EOF
no-tty
pinentry-mode loopback
EOF

  echo ">>> Importing secret key ..."
  echo "$GPG_SECRET_KEY" > seckey.gpg
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
  mv agent/build/libs/splunk-otel-javaagent-*-all.jar dist/splunk-otel-javaagent-all.jar
  mv agent/build/libs/splunk-otel-javaagent-*-all.jar.asc dist/splunk-otel-javaagent-all.jar.asc

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
    gpg --batch --passphrase-fd 0 --armor --detach-sign "$file"
}

create_gh_release() {
  echo ">>> Creating GitHub release $release_tag ..."
  gh release create "$release_tag" dist/* \
    --repo "signalfx/splunk-otel-java" \
    --draft \
    --title "Release $release_tag"
}

build_docker_image() {
  docker build -t splunk-otel-instrumentation-java .
  docker tag splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag
  docker tag splunk-otel-instrumentation-java quay.io/signalfx/splunk-otel-instrumentation-java:latest
}

publish_docker_image() {
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:latest
  docker push quay.io/signalfx/splunk-otel-instrumentation-java:$release_tag
}

login_to_quay_io() {
  docker login -u $QUAY_USERNAME -p $QUAY_PASSWORD quay.io
}

import_gpg_keys
build_project
create_gh_release
build_docker_image
login_to_quay_io
publish_docker_image
