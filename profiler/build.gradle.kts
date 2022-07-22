import com.google.protobuf.gradle.*

plugins {
  id("com.google.protobuf") version "0.8.19"
}

val protobufVersion = "3.21.3"

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protobufVersion"
  }
}

// https://youtrack.jetbrains.com/issue/IDEA-209418
// sometimes intellij doesn't detect generated protobuf classes
sourceSets {
  main {
    java {
      srcDirs("build/generated/source/proto/main/java")
    }
  }
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  // required to access InstrumentationHolder
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  implementation("io.opentelemetry:opentelemetry-sdk-logs")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-logs")
  implementation("com.google.protobuf:protobuf-java:$protobufVersion")

  compileOnly("org.slf4j:slf4j-api")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("org.slf4j:slf4j-api")
  testImplementation("io.grpc:grpc-netty")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-context")
  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.github.netmikey.logunit:logunit-logback:1.1.3")
}

tasks {
  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  compileTestJava {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
  }
}
