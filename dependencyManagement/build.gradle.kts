plugins {
  `java-platform`
}

val otelVersion = "1.23.1"
val otelAlphaVersion = "1.23.1-alpha"
val otelInstrumentationVersion = "1.23.0"
val otelInstrumentationAlphaVersion = "1.23.0-alpha"
val otelContribAlphaVersion = "1.22.0-alpha"

val micrometerVersion = "1.10.4"
val micrometerOldVersion = "1.3.20"
val dockerJavaVersion = "3.2.14"
val mockitoVersion = "5.1.1"
val slfVersion = "2.0.6"
val autoValueVersion = "1.10.1";

// instrumentation version is used to compute Implementation-Version manifest attribute
rootProject.extra["otelInstrumentationVersion"] = otelInstrumentationVersion
rootProject.extra["micrometerOldVersion"] = micrometerOldVersion

javaPlatform {
  // What a great hack!
  allowDependencies()
}

dependencies {

  // BOMs
  api(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.14.2"))
  api(enforcedPlatform("com.google.protobuf:protobuf-bom:3.21.12"))
  api(enforcedPlatform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
  api(enforcedPlatform("io.grpc:grpc-bom:1.53.0"))
  api(platform("io.micrometer:micrometer-bom:$micrometerVersion"))
  api(enforcedPlatform("io.opentelemetry:opentelemetry-bom-alpha:$otelAlphaVersion"))
  api(enforcedPlatform("io.opentelemetry:opentelemetry-bom:$otelVersion"))
  api(enforcedPlatform("org.junit:junit-bom:5.9.2"))
  api(enforcedPlatform("org.testcontainers:testcontainers-bom:1.17.6"))

  constraints {
    api("com.google.auto.service:auto-service:1.0.1")
    api("io.jaegertracing:jaeger-client:1.8.1")
    api("org.assertj:assertj-core:3.24.2")
    api("org.awaitility:awaitility:4.2.0")
    api("com.signalfx.public:signalfx-java:1.0.28")

    api("com.github.docker-java:docker-java-core:$dockerJavaVersion")
    api("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaVersion")

    api("org.mockito:mockito-core:$mockitoVersion")
    api("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    api("org.slf4j:slf4j-api:$slfVersion")
    api("org.slf4j:slf4j-simple:$slfVersion")
    api("com.google.auto.value:auto-value:$autoValueVersion")
    api("com.google.auto.value:auto-value-annotations:$autoValueVersion")

    // otel-java-instrumentation
    api("io.opentelemetry.javaagent:opentelemetry-javaagent:$otelInstrumentationVersion")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:$otelInstrumentationAlphaVersion")

    api("io.opentelemetry.javaagent:opentelemetry-agent-for-testing:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-instrumentation-api:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent:opentelemetry-muzzle:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent:opentelemetry-testing-common:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.instrumentation:opentelemetry-netty-4.1:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-3.8:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1-common:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0:$otelInstrumentationAlphaVersion")
    api("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common:$otelInstrumentationAlphaVersion")

    api("io.opentelemetry.contrib:opentelemetry-samplers:$otelContribAlphaVersion")
    api("io.opentelemetry.contrib:opentelemetry-resource-providers:$otelContribAlphaVersion")

    api("io.opentelemetry.proto:opentelemetry-proto:0.19.0-alpha")
  }

}
