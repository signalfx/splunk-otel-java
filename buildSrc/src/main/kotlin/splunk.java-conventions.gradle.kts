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

val otelVersion = "1.4.1"
val otelAlphaVersion = "1.4.1-alpha"
val otelInstrumentationVersion = "1.5.0-SNAPSHOT"
val otelInstrumentationAlphaVersion = "1.5.0-alpha-SNAPSHOT"

// dependencyManagement can't into classifiers, we have to pass version the old way for deps with qualifiers
extra["otelInstrumentationVersion"] = otelInstrumentationVersion

extensions.configure<DependencyManagementExtension>("dependencyManagement") {
  dependencies {
    dependency("com.google.auto.service:auto-service:1.0")
    dependency("com.squareup.okhttp3:okhttp:3.12.12")
    dependency("org.assertj:assertj-core:3.20.2")
    dependency("org.awaitility:awaitility:4.1.0")
    dependency("org.testcontainers:testcontainers:1.15.3")
    dependency("io.jaegertracing:jaeger-client:1.6.0")

    dependencySet("com.github.docker-java:3.2.11") {
      entry("docker-java-core")
      entry("docker-java-transport-httpclient5")
    }
    dependencySet("com.google.protobuf:3.17.3") {
      entry("protobuf-java")
      entry("protobuf-java-util")
    }
    dependencySet("org.mockito:3.8.0") {
      entry("mockito-core")
      entry("mockito-junit-jupiter")
    }
    dependencySet("org.slf4j:1.7.30") {
      entry("slf4j-api")
      entry("slf4j-simple")
    }

    // otel-java-instrumentation
    dependency("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${otelInstrumentationAlphaVersion}")
    dependencySet("io.opentelemetry.javaagent:${otelInstrumentationAlphaVersion}") {
      entry("opentelemetry-agent-for-testing")
      entry("opentelemetry-javaagent-bootstrap")
      entry("opentelemetry-javaagent-extension-api")
      entry("opentelemetry-javaagent-instrumentation-api")
      entry("opentelemetry-testing-common")
    }
    dependencySet("io.opentelemetry.javaagent.instrumentation:${otelInstrumentationAlphaVersion}") {
      entry("opentelemetry-javaagent-netty-3.8")
      entry("opentelemetry-javaagent-netty-4.0")
      entry("opentelemetry-javaagent-netty-4.1")
      entry("opentelemetry-javaagent-servlet-2.2")
      entry("opentelemetry-javaagent-servlet-3.0")
      entry("opentelemetry-javaagent-servlet-common")
    }
  }

  imports {
    mavenBom("io.grpc:grpc-bom:1.38.0")
    mavenBom("io.micrometer:micrometer-bom:1.7.1")
    mavenBom("io.opentelemetry:opentelemetry-bom-alpha:${otelAlphaVersion}")
    mavenBom("io.opentelemetry:opentelemetry-bom:${otelVersion}")
    mavenBom("org.junit:junit-bom:5.7.2")
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
