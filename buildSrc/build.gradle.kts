plugins {
  `kotlin-dsl`

  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "6.9.0"
}

spotless {
  kotlinGradle {
    ktlint().editorConfigOverride(mapOf("indent_size" to "2", "continuation_indent_size" to "2", "disabled_rules" to "no-wildcard-imports"))
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

  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
  implementation("io.opentelemetry.instrumentation:gradle-plugins:1.20.0-alpha-SNAPSHOT")
  implementation("io.spring.gradle:dependency-management-plugin:1.1.0")

  // keep these versions in sync with settings.gradle.kts
  implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
}
