plugins {
  id("java-library")
}

dependencies {
  api("io.opentelemetry.javaagent:opentelemetry-testing-common")
  api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  api("com.squareup.okhttp3:okhttp")
  api("org.mockito:mockito-core")
  api("io.opentelemetry:opentelemetry-sdk-extension-declarative-config")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("io.opentelemetry.instrumentation:opentelemetry-declarative-config-bridge")
}

tasks {
  java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  compileTestJava {
    sourceCompatibility = JavaVersion.VERSION_17.toString()
    targetCompatibility = JavaVersion.VERSION_17.toString()
  }
}
