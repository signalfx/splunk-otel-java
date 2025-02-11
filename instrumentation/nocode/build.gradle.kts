plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("org.snakeyaml:snakeyaml-engine:2.8")
}

tasks.withType<Test>().configureEach {
  environment("SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE", "./src/test/config/nocode.yml")
}
