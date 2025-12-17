plugins {
  id("com.google.protobuf") version "0.9.6"
}

protobuf {
  protoc {
    val protobufVersion = rootProject.extra["protobufVersion"]
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
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-declarative-config-bridge")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  // required to access InstrumentationHolder
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  implementation("io.opentelemetry:opentelemetry-sdk-logs")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("com.google.protobuf:protobuf-java")
  implementation("org.openjdk.jmc:flightrecorder:8.3.1") {
    exclude(group = "org.lz4", module = "lz4-java")
  }

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  testImplementation(project(":custom"))
  testImplementation(project(":testing:common"))
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
  testImplementation("io.grpc:grpc-netty")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-context")
  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.github.netmikey.logunit:logunit-jul:2.0.0")
}

tasks {
  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  compileTestJava {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
  }
}
