plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation(gradleApi())

  // keep these versions in sync with settings.gradle.kts
  implementation("com.diffplug.spotless:spotless-plugin-gradle:5.14.0")
  implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
  implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")
}
