#!/usr/bin/env bash

set -e

VERSION=$1
CSA_VERSION=$2
if [ "" == "$VERSION" ] ; then
  echo "Missing version parameter to $0"
  exit 1
fi
if [ "" == "$CSA_VERSION" ] ; then
  echo "Missing CSA version param to $0"
  exit 1
fi
echo "Building version $VERSION for CSA version $CSA_VERSION"

export CSA_EXT_CONFIG_PROPERTIES="otel-extension-system.properties"
export SPLUNK_FAT_JAR="splunk-otel-javaagent-${VERSION}.jar"
export CSA_EXT_JAR="oss-agent-mtagent-extension-deployment.jar"

export BUILD_DIR=build
export WORKING_DIR=${BUILD_DIR}/temp
export SPLUNK_TEMP=${WORKING_DIR}/splunk
export CSA_TEMP=${WORKING_DIR}/csa
export ADAPTORS_DIR="${WORKING_DIR}/inst/com/cisco/mtagent/adaptors"
export OUTPUT_JAR="${BUILD_DIR}/splunk-otel-javaagent-csa-${VERSION}.jar"

rm -rf "${BUILD_DIR}" "${WORKING_DIR}" "${SPLUNK_TEMP}" "${CSA_TEMP}"
mkdir -p "${SPLUNK_TEMP}"
mkdir -p "${CSA_TEMP}"
mkdir -p "${ADAPTORS_DIR}"

# Fetch CSA extension from releases repo
curl --fail --show-error -o \
  ${WORKING_DIR}/${CSA_EXT_JAR} \
  https://github.com/signalfx/csa-releases/releases/download/${CSA_VERSION}/${CSA_EXT_JAR}

# copy splunk-otel-java jar
MYDIR=`dirname $0`
cp ${MYDIR}/../agent/build/libs/${SPLUNK_FAT_JAR} ${OUTPUT_JAR}

cp ${CSA_EXT_CONFIG_PROPERTIES} ${WORKING_DIR}/${CSA_EXT_CONFIG_PROPERTIES}

# unpack csa jar into csa dir
unzip -o "${WORKING_DIR}/${CSA_EXT_JAR}" -d "${CSA_TEMP}" \
  com/cisco/mtagent/adaptors/AgentOSSAgentExtension.class \
  com/cisco/mtagent/adaptors/AgentOSSAgentExtensionUtil.class

# add extension interface and util/helper
cp ${CSA_TEMP}/com/cisco/mtagent/adaptors/AgentOSSAgentExtension.class \
  ${ADAPTORS_DIR}/AgentOSSAgentExtension.classdata
cp ${CSA_TEMP}/com/cisco/mtagent/adaptors/AgentOSSAgentExtensionUtil.class \
  ${ADAPTORS_DIR}/AgentOSSAgentExtensionUtil.classdata
jar uf ${OUTPUT_JAR} -C "${WORKING_DIR}"  \
  inst/com/cisco/mtagent/adaptors/AgentOSSAgentExtension.classdata
jar uf ${OUTPUT_JAR} -C "${WORKING_DIR}" \
  inst/com/cisco/mtagent/adaptors/AgentOSSAgentExtensionUtil.classdata

# add extension to service loader
unzip -o "${OUTPUT_JAR}" -d "${WORKING_DIR}" \
  inst/META-INF/services/io.opentelemetry.javaagent.extension.AgentListener
echo "com.cisco.mtagent.adaptors.AgentOSSAgentExtension" \
  >> "${WORKING_DIR}/inst/META-INF/services/io.opentelemetry.javaagent.extension.AgentListener"
jar uf "${OUTPUT_JAR}" -C "${WORKING_DIR}" \
  inst/META-INF/services/io.opentelemetry.javaagent.extension.AgentListener

# add config properties
jar uf "${OUTPUT_JAR}" -C "${WORKING_DIR}" "${CSA_EXT_CONFIG_PROPERTIES}"
# add csa extension jar to splunk fat jar
jar uf "${OUTPUT_JAR}" -C "${WORKING_DIR}" "${CSA_EXT_JAR}"

# add csa version to manifest
echo "Cisco-Secure-Application-Version: ${CSA_VERSION}" >> "${WORKING_DIR}/MANIFEST.MF"
jar -u -m "${WORKING_DIR}/MANIFEST.MF" -f "${OUTPUT_JAR}"

# show modifications
echo
echo "====== Modifications to Splunk Fat Jar Here ====>"
jar tf "${OUTPUT_JAR}" | grep "${CSA_EXT_CONFIG_PROPERTIES}"
jar tf "${OUTPUT_JAR}" | grep "${CSA_EXT_JAR}"
jar tf "${OUTPUT_JAR}" | grep AgentOSSAgentExtension
jar tf "${OUTPUT_JAR}" | grep services/io.opentelemetry.javaagent.extension.AgentListener
jar tf "${OUTPUT_JAR}" | grep ^META-INF/MANIFEST.MF
echo "====== Modifications Done."
echo
