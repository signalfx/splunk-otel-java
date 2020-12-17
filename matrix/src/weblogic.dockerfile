ARG jdk
ARG version
FROM oracle/weblogic:$version-$jdk

ARG APPLICATION_NAME
ARG APPLICATION_FILE

ENV ORACLE_HOME=/u01/oracle \
    PROPERTIES_FILE_DIR="/u01/oracle/properties" \
    DOMAIN_NAME="domain1" \
    DOMAIN_HOME="/u01/oracle/user_projects/domains/${DOMAIN_NAME}" \
    ADMIN_PORT="8080" \
    PATH=$PATH:/u01/oracle/oracle_common/common/bin:/u01/oracle/wlserver/common/bin:${DOMAIN_HOME}:${DOMAIN_HOME}/bin:/u01/oracle

COPY --chown=oracle:root container-scripts/* /u01/oracle/

USER root
RUN chmod +xw /u01/oracle/*.sh && \
    chmod +xw /u01/oracle/*.py && \
    mkdir -p ${PROPERTIES_FILE_DIR} && \
    mkdir -p $DOMAIN_HOME && \
    chown -R oracle:root $DOMAIN_HOME/.. && \
    chmod -R 775 $DOMAIN_HOME/.. && \
    chown -R oracle:root ${PROPERTIES_FILE_DIR}

COPY --chown=oracle:root properties/docker-build/domain*.properties ${PROPERTIES_FILE_DIR}/

USER oracle
RUN /u01/oracle/createDomain.sh && \
    chmod -R a+x $DOMAIN_HOME/bin/*.sh  && \
    echo ". $DOMAIN_HOME/bin/setDomainEnv.sh" >> /u01/oracle/.bashrc

COPY app.war $DOMAIN_HOME/autodeploy/app.war

EXPOSE $ADMIN_PORT

WORKDIR $DOMAIN_HOME

# Define default command to start bash.
CMD ["startAdminServer.sh"]