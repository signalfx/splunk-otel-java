#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# shellcheck source-path=SCRIPTDIR
source "${SCRIPT_DIR}/common.sh"

ROOT_DIR="${SCRIPT_DIR}/../"
cd "${ROOT_DIR}"

print_usage() {
  cat <<EOF
Usage: $(basename "$0") splunk_old_version splunk_new_version
    [--otel-instrumentation otel_instrumentation_old_version otel_instrumentation_new_version]
    [--otel otel_old_version otel_new_version]

This projects old and new versions are mandatory arguments; OTel instrumentation
and API versions are optional.
All versions MUST NOT begin with 'v'. Example: 1.2.3".
EOF
}

if [[ $# -lt 2 ]]
then
  print_usage
  exit 1
fi

splunk_old_version=$1
splunk_new_version=$2
shift 2

validate_version "$splunk_old_version"
validate_version "$splunk_new_version"

bump_otel_instrumentation_version=0
otel_instrumentation_old_version=''
otel_instrumentation_new_version=''
bump_otel_version=0
otel_old_version=''
otel_new_version=''

while [[ "$#" -gt 0 ]]
do
  case "$1" in
    --otel-instrumentation)
      bump_otel_instrumentation_version=1
      otel_instrumentation_old_version=${2}
      otel_instrumentation_new_version=${3}
      shift 3
      ;;

    --otel)
      bump_otel_version=1
      otel_old_version=${2}
      otel_new_version=${3}
      shift 3
      ;;

    *)
      echo "Unrecognized option: $1"
      print_usage
      exit 1
      ;;
  esac
done

# MacOS requires passing backup file extension to in-place sed
if [[ $(uname -s) == "Darwin" ]]
then
  sed_flag='-i.tmp'
else
  sed_flag='-i'
fi

# version.gradle.kts
sed ${sed_flag} \
  -e "s/val distroVersion = \"[^\"]*\"/val distroVersion = \"${splunk_new_version}\"/" \
   version.gradle.kts

# README.md
readme_sed_args=(
  # update version placeholders
  -e "s/<!--SPLUNK_VERSION-->${splunk_old_version}<!--SPLUNK_VERSION-->/<!--SPLUNK_VERSION-->${splunk_new_version}<!--SPLUNK_VERSION-->/g"

  # remove development version docs warning
  -e '/<!--DEV_DOCS_WARNING_START-->/,/<!--DEV_DOCS_WARNING_END-->/d'
)
if [[ $bump_otel_instrumentation_version == 1 ]]
then
  readme_sed_args+=(
    # update version placeholders
    -e "s/<!--OTEL_INSTRUMENTATION_VERSION-->${otel_instrumentation_old_version}<!--OTEL_INSTRUMENTATION_VERSION-->/<!--OTEL_INSTRUMENTATION_VERSION-->${otel_instrumentation_new_version}<!--OTEL_INSTRUMENTATION_VERSION-->/g"

    # update upstream otel link & badge
    -e "s https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v${otel_instrumentation_old_version} https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v${otel_instrumentation_new_version} g"
    -e "s https://img.shields.io/badge/otel-${otel_instrumentation_old_version} https://img.shields.io/badge/otel-${otel_instrumentation_new_version} g"
  )
fi
if [[ $bump_otel_version == 1 ]]
then
  readme_sed_args+=(
    # update version placeholders
    -e "s/<!--OTEL_VERSION-->${otel_old_version}<!--OTEL_VERSION-->/<!--OTEL_VERSION-->${otel_new_version}<!--OTEL_VERSION-->/g"
  )
fi

sed ${sed_flag} "${readme_sed_args[@]}" README.md

# CHANGELOG.md
cat > changelog_new_release.md.tmp <<EOF

## v${splunk_new_version} - $(date "+%Y-%m-%d")
EOF

sed ${sed_flag} \
  -e "/## Unreleased/r changelog_new_release.md.tmp" \
  CHANGELOG.md

rm changelog_new_release.md.tmp

# deployments/cloudfoundry/buildpack/README.md
sed ${sed_flag} \
  -e "s/SPLUNK_OTEL_JAVA_VERSION ${splunk_old_version}/SPLUNK_OTEL_JAVA_VERSION ${splunk_new_version}/g" \
   deployments/cloudfoundry/buildpack/README.md
