plugins {
  id("splunk.instrumentation-conventions")
}


dependencies {
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("org.snakeyaml:snakeyaml-engine:2.8")
  //implementation("org.yaml:snakeyaml:2.3")
  //implementation("org.snakeyaml:snakeyaml-engine:2.8")
}
