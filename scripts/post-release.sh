#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"
cd ${ROOT_DIR}

print_usage() {
  cat <<EOF
Usage: $(basename $0) splunk_current_version splunk_next_version"

The splunk_current_version parameter denotes the current, freshly released version.
The splunk_next_version parameter denotes the next release after that.

All versions MUST NOT begin with 'v'. Example: 1.2.3".
EOF
}

if [[ $# < 2 ]]
then
  print_usage
  exit 1
fi

splunk_current_version=$1
splunk_next_version=$2

# MacOS requires passing backup file extension to in-place sed
if [[ $(uname -s) == "Darwin" ]]
then
  sed_flag='-i.tmp'
else
  sed_flag='-i'
fi

# Prepare development version docs warning for the new version
cat > dev_docs_warning.md.tmp <<EOF
<!--DEV_DOCS_WARNING_START-->
The documentation below refers to the in development version of this package. Docs for the latest version ([v${splunk_current_version}](https://github.com/signalfx/splunk-otel-java/releases/latest)) can be found [here](https://github.com/signalfx/splunk-otel-java/blob/v${splunk_current_version}/README.md).

---
<!--DEV_DOCS_WARNING_END-->
EOF

readme_sed_args=(
  -e "/<!--DEV_DOCS_WARNING-->/r dev_docs_warning.md.tmp"

  # update SNAPSHOT link
  -e "s https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/${splunk_current_version}-SNAPSHOT/ https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/${splunk_next_version}-SNAPSHOT/ g"
)

sed ${sed_flag} "${readme_sed_args[@]}" README.md

rm dev_docs_warning.md.tmp
