ARG jdk
FROM adoptopenjdk:${jdk}-hotspot-focal

# Create a user and group used to launch processes
# The user ID 1000 is the default for the first "regular" user on Fedora/RHEL,
# so there is a high chance that this ID will be equal to the current user
# making it easier to use volumes (no permission issues)
RUN groupadd -r jboss -g 1000 && useradd -u 1000 -r -g jboss -m -d /opt/jboss -s /sbin/nologin -c "JBoss user" jboss && \
    chmod 755 /opt/jboss && \
    apt-get update && \
    apt-get install unzip

# Set the working directory to jboss' user home directory

# Specify the user which should be used to execute all commands below
#USER jboss

# Set the JBOSS_EAP_VERSION env variable
ARG version
ENV JBOSS_EAP_VERSION=${version}
ENV JBOSS_HOME /opt/jboss/jboss-eap
ENV JBOSS_INSTALL_DIR /opt/jboss

#USER root
COPY jboss-eap-${JBOSS_EAP_VERSION}.zip $JBOSS_INSTALL_DIR

# Add the EAS distribution to /opt, and make wildfly the owner of the extracted tar content
# Make sure the distribution is available from a well-known place

RUN unzip $JBOSS_INSTALL_DIR/jboss-eap-${JBOSS_EAP_VERSION}.zip -d $JBOSS_INSTALL_DIR \
    && rm $JBOSS_INSTALL_DIR/jboss-eap-${JBOSS_EAP_VERSION}.zip \
    && mv $JBOSS_INSTALL_DIR/jboss-eap-* $JBOSS_HOME

COPY app.war ${JBOSS_HOME}/standalone/deployments/

RUN chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}

USER jboss

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

WORKDIR /opt/jboss/jboss-eap

# Expose the ports we're interested in
EXPOSE 8080

# Set the default command to run on boot
# This will boot WildFly in the standalone mode and bind to all interface
CMD ["./bin/standalone.sh", "-b", "0.0.0.0"]

