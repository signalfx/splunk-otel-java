
# Agent-CSA Bundle

> :construction: &nbsp;Status: Experimental

This directory exists purely to contain scripts/toold to build
and publish a module that contains the Cisco Secure Application (CSA)
extension bundled with `splunk-otel-java`.

* group: `com.splunk`
* artifact: `splunk-otel-javaagent-csa`

## Build locally:

Requirements:
* Internet
* Docker client
* Java `jar` command in path
* common unix tools

From the root of the project:

```bash
$ ./gradlew agent-csa-bundle:assemble
```

The resulting bundle will be located at
`agent-csa-bundle/build/splunk-otel-javaagent-csa-<version>.jar`.
