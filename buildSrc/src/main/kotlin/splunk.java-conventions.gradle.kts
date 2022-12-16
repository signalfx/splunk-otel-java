import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
  java

  id("io.spring.dependency-management")
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
  }
}

val otelVersion = "1.21.0"
val otelAlphaVersion = "1.21.0-alpha"
val otelContribAlphaVersion = "1.21.0-alpha"
val otelInstrumentationVersion = "1.21.0"
val otelInstrumentationAlphaVersion = "1.21.0-alpha"
val micrometerVersion = "1.10.2"

// instrumentation version is used to compute Implementation-Version manifest attribute
extra["otelInstrumentationVersion"] = otelInstrumentationVersion

extensions.configure<DependencyManagementExtension>("dependencyManagement") {
  dependencies {
    dependency("com.google.auto.service:auto-service:1.0.1")
    dependency("org.assertj:assertj-core:3.23.1")
    dependency("org.awaitility:awaitility:4.2.0")
    dependency("io.jaegertracing:jaeger-client:1.8.1")
    dependency("com.signalfx.public:signalfx-java:1.0.27")

    dependencySet("com.github.docker-java:3.2.11") {
      entry("docker-java-core")
      entry("docker-java-transport-httpclient5")
    }
    dependencySet("org.mockito:4.9.0") {
      entry("mockito-core")
      entry("mockito-junit-jupiter")
    }
    dependencySet("org.slf4j:2.0.6") {
      entry("slf4j-api")
      entry("slf4j-simple")
    }
    dependencySet("com.google.auto.value:1.10.1") {
      entry("auto-value")
      entry("auto-value-annotations")
    }

    // otel-java-instrumentation
    dependency("io.opentelemetry.javaagent:opentelemetry-javaagent:$otelInstrumentationVersion")
    dependency("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelInstrumentationVersion")
    dependency("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:$otelInstrumentationAlphaVersion")
    dependencySet("io.opentelemetry.javaagent:$otelInstrumentationAlphaVersion") {
      entry("opentelemetry-agent-for-testing")
      entry("opentelemetry-javaagent-bootstrap")
      entry("opentelemetry-javaagent-extension-api")
      entry("opentelemetry-javaagent-instrumentation-api")
      entry("opentelemetry-javaagent-tooling")
      entry("opentelemetry-muzzle")
      entry("opentelemetry-testing-common")
    }
    dependencySet("io.opentelemetry.instrumentation:$otelInstrumentationAlphaVersion") {
      entry("opentelemetry-netty-4.1")
    }
    dependencySet("io.opentelemetry.javaagent.instrumentation:$otelInstrumentationAlphaVersion") {
      entry("opentelemetry-javaagent-netty-3.8")
      entry("opentelemetry-javaagent-netty-4.0")
      entry("opentelemetry-javaagent-netty-4.1")
      entry("opentelemetry-javaagent-netty-4.1-common")
      entry("opentelemetry-javaagent-servlet-2.2")
      entry("opentelemetry-javaagent-servlet-3.0")
      entry("opentelemetry-javaagent-servlet-common")
    }
    dependencySet("io.opentelemetry.contrib:$otelContribAlphaVersion") {
      entry("opentelemetry-samplers")
      entry("opentelemetry-resource-providers")
    }

    dependency("io.opentelemetry.proto:opentelemetry-proto:0.19.0-alpha")
  }

  imports {
    mavenBom("com.fasterxml.jackson:jackson-bom:2.14.1")
    mavenBom("com.google.protobuf:protobuf-bom:3.21.12")
    mavenBom("com.squareup.okhttp3:okhttp-bom:4.10.0")
    mavenBom("io.grpc:grpc-bom:1.51.0")
    mavenBom("io.micrometer:micrometer-bom:$micrometerVersion")
    mavenBom("io.opentelemetry:opentelemetry-bom-alpha:$otelAlphaVersion")
    mavenBom("io.opentelemetry:opentelemetry-bom:$otelVersion")
    mavenBom("org.junit:junit-bom:5.9.1")
    mavenBom("org.testcontainers:testcontainers-bom:1.17.6")
  }
}

dependencies {
  add("testImplementation", "org.assertj:assertj-core")
  add("testImplementation", "org.awaitility:awaitility")
  add("testImplementation", "org.mockito:mockito-core")
  add("testImplementation", "org.mockito:mockito-junit-jupiter")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
  add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
  add("testRuntimeOnly", "org.slf4j:slf4j-api:2.0.6")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  reports {
    junitXml.isOutputPerTestCase = true
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.isDeprecation = true
}
