plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()

  // for otel upstream snapshots
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
  }
}

dependencies {
  implementation(gradleApi())

  implementation("com.diffplug.spotless:spotless-plugin-gradle:5.16.0")
  implementation("io.opentelemetry.instrumentation:gradle-plugins:1.9.1-alpha")
  implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")

  // keep these versions in sync with settings.gradle.kts
  implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
}
