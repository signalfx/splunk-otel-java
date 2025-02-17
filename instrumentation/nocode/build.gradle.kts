plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("org.snakeyaml:snakeyaml-engine:2.8")

  add("codegen", project(":bootstrap"))
}

tasks.withType<Test>().configureEach {
  environment("SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE", "./src/test/config/nocode.yml")
}
