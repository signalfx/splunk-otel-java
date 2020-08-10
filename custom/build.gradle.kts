plugins {
  java
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk:0.7.0")
  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-otlp:0.7.0")
  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-jaeger:0.7.0")
  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-zipkin:0.7.0")
  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-logging:0.7.0")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-auto-config:0.7.0")
}

tasks {
  shadowJar {
    mergeServiceFiles()

    exclude("**/module-info.class")

    // Prevents conflict with other SLF4J instances. Important for premain.
    relocate("org.slf4j", "io.opentelemetry.auto.slf4j")
    // rewrite dependencies calling Logger.getLogger
    relocate("java.util.logging.Logger", "io.opentelemetry.auto.bootstrap.PatchLogger")

    // relocate OpenTelemetry API
    relocate("io.opentelemetry.OpenTelemetry", "io.opentelemetry.auto.shaded.io.opentelemetry.OpenTelemetry")
    relocate("io.opentelemetry.common", "io.opentelemetry.auto.shaded.io.opentelemetry.common")
    relocate("io.opentelemetry.context", "io.opentelemetry.auto.shaded.io.opentelemetry.context")
    relocate("io.opentelemetry.correlationcontext", "io.opentelemetry.auto.shaded.io.opentelemetry.correlationcontext")
    relocate("io.opentelemetry.internal", "io.opentelemetry.auto.shaded.io.opentelemetry.internal")
    relocate("io.opentelemetry.metrics", "io.opentelemetry.auto.shaded.io.opentelemetry.metrics")
    relocate("io.opentelemetry.trace", "io.opentelemetry.auto.shaded.io.opentelemetry.trace")
    relocate("io.opentelemetry.contrib.auto.annotations", "io.opentelemetry.auto.shaded.io.opentelemetry.contrib.auto.annotations")

    // relocate OpenTelemetry API dependency
    relocate("io.grpc", "io.opentelemetry.auto.shaded.io.grpc")
  }
}