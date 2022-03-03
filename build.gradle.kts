import nebula.plugin.release.git.opinion.Strategies

plugins {
  id("idea")
  id("nebula.release")
  id("io.github.gradle-nexus.publish-plugin")
}

release {
  defaultVersionStrategy = Strategies.getSNAPSHOT()
}

nebulaRelease {
  addReleaseBranchPattern("""v\d+\.\d+\.x""")
}

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

  apply(plugin = "splunk.java-conventions")
  apply(plugin = "splunk.spotless-conventions")
}

