#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="${SCRIPT_DIR}/../"
cd ${ROOT_DIR}

print_usage() {
  cat <<EOF
Usage: $(basename $0) splunk_new_version"

All versions MUST NOT begin with 'v'. Example: 1.2.3".
EOF
}

if [[ $# != 1 ]]
then
  print_usage
  exit 1
fi

splunk_new_version=$1

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
The documentation below refers to the in development version of this package. Docs for the latest version ([v${splunk_new_version}](https://github.com/signalfx/splunk-otel-java/releases/tag/v${splunk_new_version})) can be found [here](https://github.com/signalfx/splunk-otel-java/blob/v${splunk_new_version}/README.md).

---
<!--DEV_DOCS_WARNING_END-->
EOF

sed ${sed_flag} \
  -e "/<!--DEV_DOCS_WARNING-->/r dev_docs_warning.md.tmp" \
  README.md

rm dev_docs_warning.md.tmp
