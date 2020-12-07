#!/bin/bash

echo "Domain Home is: " $DOMAIN_HOME

export AS_HOME="${DOMAIN_HOME}/servers/${ADMIN_NAME}"
export AS_SECURITY="${AS_HOME}/security"

echo "Admin Server Home: ${AS_HOME}"
echo "Admin Server Security: ${AS_SECURITY}"

SEC_PROPERTIES_FILE=${PROPERTIES_FILE_DIR}/domain_security.properties
if [ ! -e "${SEC_PROPERTIES_FILE}" ]; then
   echo "A security.properties file with the username and password needs to be supplied."
   exit
fi

# Use the Env JAVA_OPTIONS if it's been set
if [ -z "$JAVA_OPTIONS" ]; then
  export JAVA_OPTIONS="-Dweblogic.StdoutDebugEnabled=false"
else
  JAVA_OPTIONS="${JAVA_OPTIONS} -Dweblogic.StdoutDebugEnabled=false"
fi
echo "Java Options: ${JAVA_OPTIONS}"

# Create domain
mkdir -p ${AS_SECURITY}
cp ${SEC_PROPERTIES_FILE} ${AS_SECURITY}/boot.properties
${DOMAIN_HOME}/bin/setDomainEnv.sh

# Need these for tail not to fail before WebLogic actually writes anything to the log
mkdir -p ${AS_HOME}/logs
touch ${AS_HOME}/logs/${ADMIN_NAME}.log

${DOMAIN_HOME}/bin/startWebLogic.sh &

# tail the Admin Server Logs
tail -f ${AS_HOME}/logs/${ADMIN_NAME}.log &

childPID=$!
wait $childPID
