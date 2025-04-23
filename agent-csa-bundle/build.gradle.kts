import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("maven-publish")
  id("signing")
  id("com.gradleup.shadow")
}

// This should be updated for every CSA release, eventually in dependencyManagement?

val csaVersion = "25.4.0-1327"
val otelInstrumentationVersion: String by rootProject.extra

base.archivesName.set("splunk-otel-javaagent-csa")

val csaReleases: Configuration by configurations.creating {
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
  runtimeOnly(project(":agent", configuration = "shadow"))
  csaReleases("signalfx:csa-releases:$csaVersion") {
    artifact {
      name = "oss-agent-mtagent-extension-deployment"
      extension = "jar"
    }
  }
}

tasks {

  // This exists purely as a hack to get the extension jar into our build dir
  val copyCsaJar by registering(ShadowJar::class) {
    archiveFileName.set("oss-agent-mtagent-extension-deployment.jar")
    configurations = listOf(csaReleases)
  }

  // Extract and rename extension classes
  val extractExtensionClasses by registering(Copy::class) {
    dependsOn(copyCsaJar)
    from(zipTree(copyCsaJar.get().outputs.files.first()))
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

  // Shadow always explodes jars, so we double-jar the jar to prevent it
  // from being exploded. Yay!
  val shadowExplodeWorkaround by registering(Jar::class) {
    dependsOn(copyCsaJar)
    destinationDirectory = file("build/shadow-explode-workaround")
    archiveBaseName = "nested-content"
    from(copyCsaJar)
  }

  // Copy service file so path on disk matches path in jar
  val copyServiceFile by registering(Copy::class) {
    dependsOn(extractExtensionClasses)
    from("build/ext-exploded/META-INF/services/")
    into("build/ext-exploded/inst/META-INF/services/")
  }

  shadowJar {
    archiveClassifier.set("")
    dependsOn(copyServiceFile, renameClasstoClassdata, shadowExplodeWorkaround)

    // Include the example properties file
    from("otel-extension-system.properties") {
      into("/")
    }

    // Add the entire extension jar
    from(shadowExplodeWorkaround.get().outputs.files.first())

    // Add the two extension class(data) files:
    from("build/ext-exploded/com/cisco/mtagent/adaptors/") {
      into("inst/com/cisco/mtagent/adaptors/")
      include("AgentOSSAgentExtension.classdata", "AgentOSSAgentExtensionUtil.classdata")
    }
    // Merge service descriptor files
    mergeServiceFiles {
      include("inst/META-INF/services/io.opentelemetry.javaagent.extension.AgentListener")
    }
    from("build/ext-exploded/") {
      include("inst/META-INF/services/io.opentelemetry.javaagent.extension.AgentListener")
    }

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

  assemble {
    dependsOn(shadowJar)
  }

  publishing {
    publications {
      register<MavenPublication>("maven") {
        artifactId = "splunk-otel-javaagent-csa"
        groupId = "com.splunk"
        version = project.version.toString()

        artifact(shadowJar)

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
