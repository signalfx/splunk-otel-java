ARG jdk
ARG majorVersion
ARG version

# Unzip in a separate container so that zip file layer is not part of final image
FROM mcr.microsoft.com/windows/servercore:1809 as builder
ARG majorVersion
ARG version
ADD https://archive.apache.org/dist/tomcat/tomcat-${majorVersion}/v${version}/bin/apache-tomcat-${version}-windows-x64.zip /server.zip
RUN ["powershell", "-Command", "expand-archive -Path /server.zip -DestinationPath /server"]

FROM winamd64/openjdk:${jdk}-jdk-windowsservercore-1809
ARG version
# Make /server the base directory to simplify all further paths
COPY --from=builder /server/apache-tomcat-${version} /server
COPY app.war /server/webapps/
WORKDIR /server/bin
CMD /server/bin/catalina.bat run
