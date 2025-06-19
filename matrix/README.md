# Test matrix for the Splunk distribution of opentelemetry java agent

This project builds docker images for the test matrix used by tests from the `smoke-test` sub-project.

Mostly smoke tests use test images built by the test matrix in `opentelemetry-java-instrumentation`.
However, some tests in Splunk distribution also test compatibility with proprietary app servers.
Images for proprietary app-servers require some steps to be performed manually by a developer
before images can be built. These manual steps usually mean that some files for the image must be
downloaded manually after agreeing to the terms of use of the proprietor. Such proprietary images
include Oracle WebLogic and JBoss EAS. Find a relevant section with details for each supported
proprietary app server below.

## Oracle WebLogic

### Prerequisites
To package the demo into WebLogic you need Docker _development_ images from the official Oracle GitHub repo:
https://github.com/oracle/docker-images
To build the images locally you will need to follow instructions (this means that certain zip files with
java and weblogic binaries have to be downloaded manually from links in the build instructions as the very
first step) and first build
[OracleJava 8](https://github.com/oracle/docker-images/blob/a71acdf19dc0730580457be346cf526933ec4cba/OracleJava/8/jdk/Dockerfile) and
[OracleJava 11](https://github.com/oracle/docker-images/blob/a71acdf19dc0730580457be346cf526933ec4cba/OracleJava/11/Dockerfile) and
then respective WebLogic versions.

#### WebLogic 12.1 and 12.2
Follow these instructions and build _developer_ (`-d` flag) image of
[WebLogic 12.2.1.4 on Java 8](https://github.com/oracle/docker-images/tree/a71acdf19dc0730580457be346cf526933ec4cba/OracleWebLogic/dockerfiles/12.2.1.4) and
[WebLogic 12.1.3 on Java 8](https://github.com/oracle/docker-images/tree/a71acdf19dc0730580457be346cf526933ec4cba/OracleWebLogic/dockerfiles/12.2.1.3)

*NB!* On MacOS run the build script without checksum verification (`-s` flag), as it uses missing MD5 checksum utility.

```
$ cd OracleWebLogic/dockerfiles
$ sh buildDockerImage.sh -v 12.2.1.4 -d -s
$ sh buildDockerImage.sh -v 12.1.3 -d -s
```
This will build the docker image `oracle/weblogic:12.2.1.4-developer` and `oracle/weblogic:12.1.3-developer`

Now, when the Base WebLogic Image is built, you can run
`./gradlew weblogicImage-12.2.1.4-jdkdeveloper weblogicImage-12.1.3-jdkdeveloper` to build the
images with the test app deployed that can be used by SmokeTests.

#### WebLogic 14
Follow these instructions and build _developer_ (`-d` flag) image of
[WebLogic 14.1.1.0](https://github.com/oracle/docker-images/tree/master/OracleWebLogic/dockerfiles/14.1.1.0)
on both Java 8 (`-j 8` flag) and Java 11 (`-j 11` flag)

*NB!* On MacOS run the build script without checksum verification (`-s` flag), as it uses missing MD5 checksum utility.

```
$ cd OracleWebLogic/dockerfiles
$ sh buildDockerImage.sh -v 14.1.1.0 -d -s -j 8
$ sh buildDockerImage.sh -v 14.1.1.0 -d -s -j 11
```

This will build docker images `oracle/weblogic:14.1.1.0-developer-8` and `oracle/weblogic:14.1.1.0-developer-11` respectively.

Now, when the Base WebLogic Image is built, you can run `./gradlew weblogicImage-14.1.1.0-jdkdeveloper-8` and
`weblogicImage-14.1.1.0-jdkdeveloper-11` to build images with the test app deployed which can be used by SmokeTests.

### JBoss EAP
To build JBoss EAP images you need to download following files to the `src/main/docker/jboss-eap` directory:

* [jboss-eap-7.1.0.zip](https://developers.redhat.com/download-manager/file/jboss-eap-7.1.0.zip)
* [jboss-eap-7.3.0.zip](https://developers.redhat.com/download-manager/file/jboss-eap-7.3.0.zip)

Now you can run gradle targets `jboss-eapImage-7.1.0-jdk8`, `jboss-eapImage-7.3.0-jdk11`, `jboss-eapImage-7.3.0-jdk8`
to build Docker images with the test app deployed which can be used by SmokeTests.
