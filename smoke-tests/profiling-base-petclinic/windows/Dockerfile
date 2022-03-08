# Dockerfile for the application under test
# Builds petclinic-rest using the specified JDK version (minimum is 8 for JFR)
ARG jdkVersion=8

FROM eclipse-temurin:${jdkVersion}-windowsservercore-1809 as builder

ADD https://github.com/spring-petclinic/spring-petclinic-rest/archive/refs/heads/master.zip /src.zip
RUN ["powershell", "-Command", "expand-archive -Path /src.zip -DestinationPath /src"]

WORKDIR /src/spring-petclinic-rest-master/
RUN ["/src/spring-petclinic-rest-master/mvnw.cmd", "-Dmaven.test.skip=true", "package"]
WORKDIR /src/spring-petclinic-rest-master/target
RUN ["powershell", "-Command", "Get-ChildItem -Path spring-petclinic-rest-*.jar | ForEach { Rename-Item $_.FullName -NewName spring-petclinic-rest.jar }"]

FROM eclipse-temurin:${jdkVersion}-windowsservercore-1809

RUN ["powershell", "-Command", "New-Item -ItemType directory -Path /app"]
COPY --from=builder /src/spring-petclinic-rest-master/target/spring-petclinic-rest.jar /app/spring-petclinic-rest.jar
WORKDIR /app
