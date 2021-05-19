#!/usr/bin/env bash

SYSTEMS=("linux" "windows")
SUITES=("glassfish" "jboss" "jetty" "liberty" "tomcat" "tomee" "weblogic" "wildfly" "other")

cat << EOF
.smoke-test: &smoke-test
  stage: test
  script:
    - ./gradlew :smoke-tests:test -PsmokeTestSuite="\$SMOKE_TEST_SUITE" --scan --no-daemon

.linux-smoke-test: &linux-smoke-test
  <<: *smoke-test
  image: "openjdk:11.0.11-9-jdk"

.windows-smoke-test: &windows-smoke-test
  <<: *smoke-test
  image: "openjdk:11.0.11-9-jdk-windowsservercore"
EOF

for os in "${SYSTEMS[@]}"
do
  for suite in "${SUITES[@]}"
  do
    cat << EOF

smoke-test-${os}-${suite}:
  <<: *${os}-smoke-test
  variables:
    SMOKE_TEST_SUITE: ${suite}
EOF
  done
done