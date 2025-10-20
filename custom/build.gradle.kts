import org.gradle.kotlin.dsl.invoke

dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-resources")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-internal-logging-simple")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  implementation("io.opentelemetry.contrib:opentelemetry-samplers")
  implementation("io.opentelemetry.contrib:opentelemetry-resource-providers")

  testImplementation(project(":testing:common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-resources")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
}

sourceSets {
  main {
    output.dir("build/generated/properties", "builtBy" to "generateVersionResource")
  }
}

tasks {
  compileJava {
    options.release.set(8)
  }

  // TODO: investigate why adding to processResources throws UnsupportedOperationException, but only in GHA
  register("generateVersionResource") {
    val propertiesDir = file("build/generated/properties")
    outputs.dir(propertiesDir)

    doLast {
      File(propertiesDir, "splunk.properties").writeText("telemetry.distro.version=${project.version}")
    }
  }
}
