
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

## Updating the bundled CSA version

The `Update bundled CSA version` GitHub Actions workflow checks the latest
release in [`signalfx/csa-releases`](https://github.com/signalfx/csa-releases)
once per day. When that release differs from the `csaVersion` in
`build.gradle.kts`, the workflow opens an automated pull request with the
update. The workflow can also be run manually from the Actions tab.
