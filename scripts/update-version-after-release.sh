#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source-path=SCRIPTDIR
source "${SCRIPT_DIR}/common.sh"

ROOT_DIR="${SCRIPT_DIR}/../"
cd "${ROOT_DIR}"

print_usage() {
  cat <<EOF
Usage: $(basename "$0") splunk_current_version splunk_next_version

The splunk_current_version parameter denotes the current, freshly released version.
The splunk_next_version parameter denotes the next release after that.

All versions MUST NOT begin with 'v', and MUST NOT contain '-SNAPSHOT' suffix. Example: 1.2.3.
EOF
}

if [[ $# -lt 2 ]]
then
  print_usage
  exit 1
fi

splunk_current_version=$1
splunk_next_version=$2

validate_version "$splunk_current_version"
validate_version "$splunk_next_version"

# MacOS requires passing backup file extension to in-place sed
if [[ $(uname -s) == "Darwin" ]]
then
  sed_flag='-i.tmp'
else
  sed_flag='-i'
fi

# version.gradle.kts
sed ${sed_flag} \
  -e "s/val distroVersion = \"[^\"]*\"/val distroVersion = \"${splunk_next_version}-SNAPSHOT\"/" \
   version.gradle.kts

# Prepare development version docs warning for the new version
cat > dev_docs_warning.md.tmp <<EOF
<!--DEV_DOCS_WARNING_START-->
The following documentation refers to the in-development version of \`splunk-otel-java\`. Docs for the latest version ([v${splunk_current_version}](https://github.com/signalfx/splunk-otel-java/releases/latest)) can be found [here](https://github.com/signalfx/splunk-otel-java/blob/v${splunk_current_version}/README.md).

---
<!--DEV_DOCS_WARNING_END-->
EOF

readme_sed_args=(
  -e "/<!--DEV_DOCS_WARNING-->/r dev_docs_warning.md.tmp"

  # update SNAPSHOT link
  -e "s https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/${splunk_current_version}-SNAPSHOT/ https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/${splunk_next_version}-SNAPSHOT/ g"
  -e "s https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/splunk/splunk-otel-javaagent/${splunk_current_version}-SNAPSHOT/ https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/splunk/splunk-otel-javaagent/${splunk_next_version}-SNAPSHOT/ g"
)

sed ${sed_flag} "${readme_sed_args[@]}" README.md

rm dev_docs_warning.md.tmp

# update main cloudfoundry buildpack index with new released version
CF_INDEX=deployments/cloudfoundry/index.yml
echo "${splunk_current_version}: https://github.com/signalfx/splunk-otel-java/releases/download/v${splunk_current_version}/splunk-otel-javaagent.jar" >> "${CF_INDEX}"
