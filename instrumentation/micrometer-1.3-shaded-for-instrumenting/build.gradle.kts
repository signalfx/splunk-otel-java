plugins {
  id("com.github.johnrengelman.shadow")
}

val micrometerOldVersion: String by rootProject.extra

dependencies {
  implementation("io.micrometer:micrometer-core")
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "io.micrometer" && requested.name == "micrometer-core") {
      useVersion(micrometerOldVersion)
      because("We need the old version")
    }
  }
}

// we need to shade the micrometer API to be able to instrument it - it's similar to how OTel bridge works
tasks {
  shadowJar {
    relocate("io.micrometer", "application.io.micrometer")
  }
}
