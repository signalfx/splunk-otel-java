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
