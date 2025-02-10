plugins {
  id("splunk.instrumentation-conventions")
}


dependencies {
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("org.snakeyaml:snakeyaml-engine:2.8")
}
