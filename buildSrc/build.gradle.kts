plugins {
  `kotlin-dsl`

  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "6.1.2"
}

spotless {
  kotlinGradle {
    ktlint().userData(mapOf("indent_size" to "2", "continuation_indent_size" to "2"))
    target("**/*.gradle.kts")
  }
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

  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.1.2")
  implementation("io.opentelemetry.instrumentation:gradle-plugins:1.10.0-alpha")
  implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")

  // keep these versions in sync with settings.gradle.kts
  implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
}
