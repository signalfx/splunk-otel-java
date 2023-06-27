plugins {
  id("idea")
  id("io.github.gradle-nexus.publish-plugin")
}

apply(from = "version.gradle.kts")

nexusPublishing {
  packageGroup.set("com.splunk")

  repositories {
    sonatype {
      username.set(System.getenv("SONATYPE_USERNAME"))
      password.set(System.getenv("SONATYPE_PASSWORD"))
    }
  }
}

group = "com.splunk"

subprojects {
  version = rootProject.version

  if (this.name != "dependencyManagement") {
    apply(plugin = "splunk.java-conventions")
    apply(plugin = "splunk.spotless-conventions")
  }
}

