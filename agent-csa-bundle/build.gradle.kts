import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("maven-publish")
  id("signing")
  id("com.gradleup.shadow")
}

// This should be updated for every CSA release, eventually in dependencyManagement?

val csaVersion = "25.10.0-1399"
val otelInstrumentationVersion: String by rootProject.extra

base.archivesName.set("splunk-otel-javaagent-csa")

java {
  withJavadocJar()
  withSourcesJar()
}

val csaReleases: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

val splunkAgent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

repositories {
  ivy {
    // Required to source artifact directly from github release page
    // https://github.com/signalfx/csa-releases/releases/download/<version>/oss-agent-mtagent-extension-deployment.jar
    url = uri("https://github.com/")
    metadataSources {
      artifact()
    }
    patternLayout {
      ivy("[organisation]/[module]/releases/download/[revision]/[artifact].[ext]")
      artifact("[organisation]/[module]/releases/download/[revision]/[artifact].[ext]")
    }
  }
}

dependencies {
  splunkAgent(project(":agent", configuration = "shadow"))
  csaReleases("signalfx:csa-releases:$csaVersion") {
    artifact {
      name = "oss-agent-mtagent-extension-deployment"
      extension = "jar"
    }
  }
}

tasks {
  // This exists purely to get the extension jar into our build dir
  val copyCsaJar by registering(Jar::class) {
    archiveFileName.set("oss-agent-mtagent-extension-deployment.jar")
    doFirst {
      from(zipTree(csaReleases.singleFile))
    }
  }

  // Extract and rename extension classes
  val extractExtensionClasses by registering(Copy::class) {
    dependsOn(copyCsaJar)
    from(zipTree(copyCsaJar.get().archiveFile))
    into("build/ext-exploded")
  }

  // Rename class to classdata
  val renameClasstoClassdata by registering(Copy::class) {
    dependsOn(extractExtensionClasses)
    from("build/ext-exploded/com/cisco/mtagent/adaptors/")
    into("build/ext-exploded/com/cisco/mtagent/adaptors/")
    include("AgentOSSAgentExtension.class", "AgentOSSAgentExtensionUtil.class")
    rename("AgentOSSAgentExtension.class", "AgentOSSAgentExtension.classdata")
    rename("AgentOSSAgentExtensionUtil.class", "AgentOSSAgentExtensionUtil.classdata")
  }

  // Copy service file so path on disk matches path in jar
  val copyServiceFile by registering(Copy::class) {
    dependsOn(extractExtensionClasses)
    from("build/ext-exploded/META-INF/services/")
    into("build/ext-exploded/inst/META-INF/services/")
  }

  val shadowCsaClasses by registering(ShadowJar::class) {
    archiveFileName.set("shadow-csa-classes.jar")
    dependsOn(copyServiceFile, renameClasstoClassdata, splunkAgent)

    doFirst {
      from(zipTree(splunkAgent.singleFile))
    }

    // Include the example properties file
    from("otel-extension-system.properties") {
      into("/")
    }

    // Add the two extension class(data) files:
    from("build/ext-exploded/com/cisco/mtagent/adaptors/") {
      into("inst/com/cisco/mtagent/adaptors/")
      include("AgentOSSAgentExtension.classdata", "AgentOSSAgentExtensionUtil.classdata")
    }
    // Defaulted here to FAIL, because we only want to merge the one
    duplicatesStrategy = DuplicatesStrategy.FAIL
    mergeServiceFiles("inst/META-INF/services")
    // mergeServiceFiles requires that duplicate strategy is set to include
    filesMatching("inst/META-INF/services/**") {
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    from("build/ext-exploded/") {
      include("inst/META-INF/services/io.opentelemetry.javaagent.extension.AgentListener")
    }
  }

  jar {
    dependsOn(shadowCsaClasses)
    from(zipTree(shadowCsaClasses.get().archiveFile.get()))
    from(copyCsaJar.get().archiveFile.get())

    manifest {
      attributes(
        mapOf(
          "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Agent-Class" to "com.splunk.opentelemetry.javaagent.SplunkAgent",
          "Premain-Class" to "com.splunk.opentelemetry.javaagent.SplunkAgent",
          "Can-Redefine-Classes" to "true",
          "Can-Retransform-Classes" to "true",
          "Implementation-Vendor" to "Splunk",
          "Implementation-Version" to "splunk-${project.version}-otel-$otelInstrumentationVersion",
          "Cisco-Secure-Application-Version" to csaVersion,
        )
      )
    }
  }

  val t = this
  publishing {
    publications {
      register<MavenPublication>("maven") {
        artifactId = "splunk-otel-javaagent-csa"
        groupId = "com.splunk"
        version = project.version.toString()

        artifact(jar)
        artifact(t.named("sourcesJar"))
        artifact(t.named("javadocJar"))

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
