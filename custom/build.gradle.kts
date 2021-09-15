dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  implementation("io.opentelemetry:opentelemetry-exporter-jaeger-thrift") {
    exclude("io.opentelemetry", "opentelemetry-sdk")
  }
  implementation("io.jaegertracing:jaeger-client")

  compileOnly("io.micrometer:micrometer-core")
  implementation("io.micrometer:micrometer-registry-signalfx") {
    // bootstrap already has micrometer-core
    exclude("io.micrometer", "micrometer-core")
  }

  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation("io.micrometer:micrometer-core")

  testImplementation(project(":testing:common"))
  testImplementation("javax.servlet:javax.servlet-api:3.0.1")
  testImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
  testImplementation("org.eclipse.jetty:jetty-servlet:8.0.0.v20110901")
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
