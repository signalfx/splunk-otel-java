plugins {
  id("com.github.johnrengelman.shadow")
}

dependencies {
  implementation("io.micrometer:micrometer-core:1.9.1")
}

// we need to shade the micrometer API to be able to instrument it - it's similar to how OTel bridge works
tasks {
  shadowJar {
    relocate("io.micrometer", "application.io.micrometer")
  }
}
