#!/usr/bin/env bash

set -e

if [[ $# < 4 ]]
then
  echo "Usage $(basename $0) splunk_old_version splunk_new_version otel_instrumentation_old_version otel_instrumentation_new_version [otel_old_version] [otel_new_version]"
  echo "  All versions MUST NOT begin with 'v'. Example: 1.2.3"
  exit 1
fi

splunk_old_version=$1
splunk_new_version=$2
otel_instrumentation_old_version=$3
otel_instrumentation_new_version=$4
otel_old_version=${5:-${otel_instrumentation_old_version}}
otel_new_version=${6:-${otel_instrumentation_new_version}}

# MacOS requires passing backup file extension to in-place sed
if [[ $(uname -s) == "Darwin" ]]
then
  sed_flag='-i.tmp'
else
  sed_flag='-i'
fi

# README.md
readme_sed_args=(
  # update version placeholders
  -e "s/<!--SPLUNK_VERSION-->${splunk_old_version}<!--SPLUNK_VERSION-->/<!--SPLUNK_VERSION-->${splunk_new_version}<!--SPLUNK_VERSION-->/g"
  -e "s/<!--OTEL_INSTRUMENTATION_VERSION-->${otel_instrumentation_old_version}<!--OTEL_INSTRUMENTATION_VERSION-->/<!--OTEL_INSTRUMENTATION_VERSION-->${otel_instrumentation_new_version}<!--OTEL_INSTRUMENTATION_VERSION-->/g"
  -e "s/<!--OTEL_VERSION-->${otel_old_version}<!--OTEL_VERSION-->/<!--OTEL_VERSION-->${otel_new_version}<!--OTEL_VERSION-->/g"

  # update upstream otel link & badge
  -e "s https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v${otel_instrumentation_old_version} https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v${otel_instrumentation_new_version} g"
  -e "s https://img.shields.io/badge/otel-${otel_instrumentation_old_version} https://img.shields.io/badge/otel-${otel_instrumentation_new_version} g"

  # remove development version docs warning
  -e '/<!--DEV_DOCS_WARNING_START-->/,/<!--DEV_DOCS_WARNING_END-->/d'
)
sed ${sed_flag} "${readme_sed_args[@]}" README.md

# deployments/cloudfoundry/buildpack/README.md
sed ${sed_flag} \
  -e "s/SPLUNK_OTEL_JAVA_VERSION ${splunk_old_version}/SPLUNK_OTEL_JAVA_VERSION ${splunk_new_version}/g" \
   deployments/cloudfoundry/buildpack/README.md
