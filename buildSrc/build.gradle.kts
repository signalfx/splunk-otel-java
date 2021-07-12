plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenLocal()
}

dependencies {
  implementation(gradleApi())

  // keep these versions in sync with settings.gradle.kts
  implementation("com.diffplug.spotless:spotless-plugin-gradle:5.14.0")
  implementation("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:7.0.0")
  implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")
  implementation("io.opentelemetry.instrumentation.gradle:opentelemetry-javaagent-muzzle-generation:1.4.0-alpha-SNAPSHOT")
  implementation("net.bytebuddy:byte-buddy-gradle-plugin:1.11.2")
  implementation("com.google.guava:guava:30.1.1-jre")

}
