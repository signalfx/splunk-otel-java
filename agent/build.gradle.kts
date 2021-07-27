import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("maven-publish")
  id("signing")
  id("splunk.shadow-conventions")
}

base.archivesName.set("splunk-otel-javaagent")

java {
  withJavadocJar()
  withSourcesJar()
}

// this configuration collects libs that will be placed in the bootstrap classloader
val bootstrapLibs by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration collects libs that will be placed in the agent classloader, isolated from the instrumented application code
val javaagentLibs by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration stores the upstream agent dep that's extended by this project
val upstreamAgent by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

val otelInstrumentationVersion: String by extra

dependencies {
  bootstrapLibs(project(":bootstrap"))

  javaagentLibs(project(":custom"))
  javaagentLibs(project(":instrumentation"))
  javaagentLibs(project(":profiler"))

  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:${otelInstrumentationVersion}:all")
}

tasks {
  jar {
    enabled = false
  }

  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

    duplicatesStrategy = DuplicatesStrategy.FAIL

    archiveFileName.set("javaagentLibs-relocated.jar")
  }

  // having a separate task for isolating javaagent libs is required to avoid duplicates
  // duplicatesStrategy in shadowJar won't be applied when adding files with with(CopySpec)
  val isolateJavaagentLibs by registering(Copy::class) {
    dependsOn(relocateJavaagentLibs)
    isolateClasses(relocateJavaagentLibs.get().outputs.files)

    into("$buildDir/isolated/javaagentLibs")
  }

  shadowJar {
    configurations = listOf(bootstrapLibs, upstreamAgent)

    dependsOn(isolateJavaagentLibs)
    from(isolateJavaagentLibs.get().outputs)

    archiveClassifier.set("all")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
      attributes(mapOf(
          "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Agent-Class" to "com.splunk.opentelemetry.javaagent.SplunkAgent",
          "Premain-Class" to "com.splunk.opentelemetry.javaagent.SplunkAgent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true,
          "Implementation-Vendor" to "Splunk",
          "Implementation-Version" to "splunk-${project.version}-otel-${otelInstrumentationVersion}"
      ))
    }
  }

  // create a no-classifier jar that's exactly the same as the -all one
  // a no-classifier (main) jar is required by sonatype
  val mainShadowJar by registering(Jar::class) {
    archiveClassifier.set("")

    from(zipTree(shadowJar.get().archiveFile))

    manifest {
      attributes(shadowJar.get().manifest.attributes)
    }
  }

  assemble {
    dependsOn(shadowJar, mainShadowJar)
  }

  val t = this
  publishing {
    publications {
      register<MavenPublication>("maven") {
        artifactId = "splunk-otel-javaagent"
        groupId = "com.splunk"
        version = project.version.toString()

        artifact(shadowJar)
        artifact(mainShadowJar)
        artifact(t.named("sourcesJar"))
        artifact(t.named("javadocJar"))

        pom {
          name.set("Splunk Distribution of OpenTelemetry Java")
          description.set("A distribution of the OpenTelemetry Instrumentation for Java project")
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

rootProject.tasks.named("release") {
  finalizedBy(tasks["publishToSonatype"])
}

fun CopySpec.isolateClasses(jars: Iterable<File>) {
  jars.forEach {
    from(zipTree(it)) {
      into("inst")
      rename("(^.*)\\.class\$", "\$1.classdata")
    }
  }
}
