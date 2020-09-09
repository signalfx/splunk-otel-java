import nebula.plugin.release.git.opinion.Strategies

plugins {
  java
  id("com.diffplug.spotless") version "5.2.0" apply false

  id("nebula.release") version "15.0.1"
}

release {
  defaultVersionStrategy = Strategies.getSNAPSHOT()
}

group = "com.signalfx.public"

subprojects {
  version = rootProject.version

  apply<JavaPlugin>()
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

  dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
  }

  tasks {
    test {
      useJUnitPlatform()
      reports {
        junitXml.isOutputPerTestCase = true
      }
    }
  }
}