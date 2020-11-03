# Demo application running in Oracle WebLogic

## Prerequisites
To package the demo into WebLogic you need Docker _development_ images from the official Oracle GitHub repo:
https://github.com/oracle/docker-images
To build the images locally you will need to follow instructions (this means that certain zip files with
java and weblogic binaries have to be downloaded manually from links in the build instructions as the very 
first step) and first build  
[OracleJava 8](https://github.com/oracle/docker-images/blob/master/OracleJava/8/Dockerfile) and 
[OracleJava 11](https://github.com/oracle/docker-images/blob/master/OracleJava/11/Dockerfile) and 
then respective WebLogic versions.
 
#### WebLogic 12
Follow these instructions and build _developer_ (`-d` flag) image of 
[WebLogic 12.2.1.4 on Java 8](https://github.com/oracle/docker-images/tree/master/OracleWebLogic/dockerfiles/12.2.1.4)

*NB!* On MacOS run the build script without checksum verification (`-s` flag), as it uses missing MD5 checksum utility.

```
$ cd OracleWebLogic/dockerfiles
$ sh buildDockerImage.sh -v 12.2.1.4 -d -s
```
This will build the docker image `oracle/weblogic:12.2.1.4-developer`

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
