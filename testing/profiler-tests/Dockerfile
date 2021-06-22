# Dockerfile for the application under test
# Builds petclinic-rest in a java 8 container new enough to support JFR
FROM adoptopenjdk:8

RUN apt update && apt install -y git
RUN git clone https://github.com/spring-petclinic/spring-petclinic-rest.git /src

WORKDIR /src
RUN ./mvnw -Dmaven.test.skip=true package

RUN mkdir /app && cp /src/target/spring-petclinic-rest*.jar /app/spring-petclinic-rest.jar
WORKDIR /app