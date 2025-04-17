
plugins {
  id("maven-publish")
  id("signing")
}

dependencies {
  implementation(project(":agent"))
}

// This should be updated for every CSA release
val csaVersion = "25.3.0-1321"
base.archivesName.set("splunk-otel-javaagent-csa")

tasks {
  val bundleSecureApp by registering(Exec::class) {
    description = "Bundle the Cisco SecureApp with splunk-otel-java"
    dependsOn(":agent:assemble")
    isIgnoreExitValue = false
    commandLine("./build-agent-csa-fat-jar.sh", version, csaVersion)
    outputs.file(File("build/splunk-otel-javaagent-csa-$version.jar"))
  }

  jar {
    inputs.files(bundleSecureApp)
  }

  assemble {
    dependsOn(bundleSecureApp)
  }

  publishing {
    publications {
      register<MavenPublication>("maven") {
        artifactId = "splunk-otel-javaagent-csa"
        groupId = "com.splunk"
        version = project.version.toString()

        artifact(bundleSecureApp)

        pom {
          name.set("splunk-otel-java with Cisco SecureApp bundle")
          description.set("A distribution of the OpenTelemetry Instrumentation for Java which includes a bundled version of the Cisco SecureApp")
          url.set("https://github.com/signalfx/splunk-otel-java")
          packaging = "jar"

          licenses {
            license {
              name.set("The Apache License, Version 2.0")
              url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }

          developers {
            developer {
              id.set("splunk")
              name.set("Splunk Instrumentation Authors")
              email.set("support+java@signalfx.com")
              organization.set("Splunk")
              organizationUrl.set("https://www.splunk.com")
            }
          }

          scm {
            connection.set("https://github.com/signalfx/splunk-otel-java.git")
            developerConnection.set("https://github.com/signalfx/splunk-otel-java.git")
            url.set("https://github.com/signalfx/splunk-otel-java")
          }
        }
      }
    }
  }

  val gpgSecretKey = System.getenv("GPG_SECRET_KEY")
  val gpgPassword = System.getenv("GPG_PASSWORD")
  if (gpgSecretKey != null && gpgPassword != null) {
    signing {
      useInMemoryPgpKeys(gpgSecretKey, gpgPassword)
      sign(publishing.publications["maven"])
    }
  }
}
