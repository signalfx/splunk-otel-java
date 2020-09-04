plugins {
  java
  id("com.diffplug.spotless") version "5.2.0" apply false
}

group = "com.signalfx.public"
version = "1.0-SNAPSHOT"

subprojects {
  version = rootProject.version

  apply(plugin = "com.diffplug.spotless")
  apply(from = "$rootDir/gradle/spotless.gradle")

  repositories {
    jcenter()
    maven {
      url = uri("https://dl.bintray.com/open-telemetry/maven")
    }
    maven {
      url = uri("https://oss.jfrog.org/artifactory/oss-snapshot-local")
    }
    mavenCentral()
  }
}