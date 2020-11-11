#!/bin/bash

DOMAIN_PROPERTIES_FILE=${PROPERTIES_FILE_DIR}/domain.properties
SEC_PROPERTIES_FILE=${PROPERTIES_FILE_DIR}/domain_security.properties
if [ ! -e "${SEC_PROPERTIES_FILE}" ]; then
   echo "A properties file ${SEC_PROPERTIES_FILE} with the username and password needs to be supplied."
   exit
fi

wlst.sh -skipWLSModuleScanning -loadProperties ${DOMAIN_PROPERTIES_FILE} -loadProperties ${SEC_PROPERTIES_FILE}  /u01/oracle/create-wls-domain.py
