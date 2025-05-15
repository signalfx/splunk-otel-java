plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support")
  compileOnly("org.snakeyaml:snakeyaml-engine:2.8")
  testImplementation("org.snakeyaml:snakeyaml-engine:2.8")

  implementation("org.apache.commons:commons-jexl3:3.4.0") {
    exclude("commons-logging", "commons-logging")
  }
  implementation("org.slf4j:jcl-over-slf4j")

  add("codegen", project(":bootstrap"))
}

tasks.withType<Test>().configureEach {
  environment("SPLUNK_OTEL_INSTRUMENTATION_NOCODE_YML_FILE", "./src/test/config/nocode.yml")
}
