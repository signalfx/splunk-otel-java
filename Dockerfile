FROM busybox

ARG JAR_FILE=splunk-otel-javaagent.jar

ADD dist/${JAR_FILE} /
