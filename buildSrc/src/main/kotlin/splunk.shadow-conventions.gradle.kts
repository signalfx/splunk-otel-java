import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar>().configureEach {
  mergeServiceFiles {
    include("inst/META-INF/services/*")
  }

  exclude("**/module-info.class")

  // Prevents conflict with other SLF4J instances. Important for premain.
  relocate("org.slf4j", "io.opentelemetry.javaagent.slf4j")
  // rewrite dependencies calling Logger.getLogger
  relocate("java.util.logging.Logger", "io.opentelemetry.javaagent.bootstrap.PatchLogger")

  // rewrite library instrumentation dependencies
  relocate("io.opentelemetry.instrumentation", "io.opentelemetry.javaagent.shaded.instrumentation") {
    // Exclude resource providers since they live in the agent class loader
    exclude("io.opentelemetry.instrumentation.resources.*")
    exclude("io.opentelemetry.instrumentation.spring.resources.*")
  }

  // relocate OpenTelemetry API usage
  relocate("io.opentelemetry.api", "io.opentelemetry.javaagent.shaded.io.opentelemetry.api")
  relocate("io.opentelemetry.semconv", "io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv")
  relocate("io.opentelemetry.context", "io.opentelemetry.javaagent.shaded.io.opentelemetry.context")

  // relocate the OpenTelemetry extensions that are used by instrumentation modules
  // these extensions live in the AgentClassLoader, and are injected into the user"s class loader
  // by the instrumentation modules that use them
  relocate("io.opentelemetry.extension.aws", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.aws")
  relocate("io.opentelemetry.extension.kotlin", "io.opentelemetry.javaagent.shaded.io.opentelemetry.extension.kotlin")

  // relocate Micrometer
  relocate("io.micrometer", "com.splunk.javaagent.shaded.io.micrometer")
  // micrometer dependencies
  relocate("org.HdrHistogram", "com.splunk.javaagent.shaded.org.hdrhistogram")
  relocate("org.LatencyUtils", "com.splunk.javaagent.shaded.org.latencyutils")

  // relocate micrometer instrumentation packages
  relocate("application.io.micrometer", "io.micrometer")
}
