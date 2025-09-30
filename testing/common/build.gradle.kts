plugins {
  id("java-library")
}

dependencies {
  compileOnly(project(":bootstrap"))

  api("io.opentelemetry.javaagent:opentelemetry-testing-common")
  api("com.squareup.okhttp3:okhttp")
  api("org.mockito:mockito-core")
  api("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  api("io.opentelemetry.instrumentation:opentelemetry-declarative-config-bridge")
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
