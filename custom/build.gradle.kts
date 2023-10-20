dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-internal-logging-simple")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  implementation("io.opentelemetry.contrib:opentelemetry-samplers")
  implementation("io.opentelemetry.contrib:opentelemetry-resource-providers")

  compileOnly("io.micrometer:micrometer-core")
  implementation("io.micrometer:micrometer-registry-signalfx") {
    // bootstrap already has micrometer-core
    exclude("io.micrometer", "micrometer-core")
    // we replace signalfx-java with signalfx-metrics
    exclude("com.signalfx.public", "signalfx-java")
  }
  implementation("com.signalfx.public:signalfx-metrics") {
    // we use jcl-over-slf4j
    exclude("commons-logging", "commons-logging")
  }
  implementation("org.slf4j:jcl-over-slf4j")

  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation("io.micrometer:micrometer-core")

  testImplementation(project(":testing:common"))
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
      File(propertiesDir, "splunk.properties").writeText("splunk.distro.version=${project.version}")
    }
  }
}
