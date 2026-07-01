
# Agent-SecureApp Bundle

> :construction: &nbsp;Status: Experimental

This directory exists purely to contain scripts/tools to build
and publish a module that contains the SecureApp
extension bundled with `splunk-otel-java`.

* group: `com.splunk`
* artifact: `splunk-otel-javaagent-secureapp`

## Build locally:

Requirements:
* Internet
* Docker client
* Java `jar` command in path
* common unix tools

From the root of the project:

```bash
$ ./gradlew agent-secureapp-bundle:assemble
```

The resulting bundle will be located at
`agent-secureapp-bundle/build/libs/splunk-otel-javaagent-secureapp-<version>.jar`.
