#/bin/sh

echo "Compiling ..."
javac TlabSanityTestApp.java
echo "Starting ..."
java -javaagent:opentelemetry-javaagent.jar TlabSanityTestApp
