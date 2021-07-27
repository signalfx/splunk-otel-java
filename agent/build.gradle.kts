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

// dependencies that already are relocated and will be moved to inst/ (agent classloader isolation)
val isolateLibs by configurations.creating
// dependencies that will be relocated
val relocateLibs by configurations.creating
// dependencies that will be included as they are
val includeAsIs by configurations.creating

configurations {
  named("compileOnly") {
    extendsFrom(includeAsIs)
  }
}

val otelInstrumentationVersion: String by extra

// TODO: remove those three lines after shadowJar plugin fixes its afterEvaluate() calls
// since gradle 7.0 calling afterEvaluate() on already configured project throws an error, and this is exactly
// what the shadowJar plugin seems to do - calling evaluationDependsOn() for all shadowed projects seems to delay
// the configuration until after the plugin is applied
evaluationDependsOn(":custom")
evaluationDependsOn(":instrumentation")
evaluationDependsOn(":profiler")

dependencies {
  isolateLibs(project(":custom", configuration = "shadow"))
  isolateLibs(project(":instrumentation", configuration = "shadow"))
  isolateLibs(project(":profiler", configuration = "shadow"))

  relocateLibs(project(":bootstrap"))

  includeAsIs("io.opentelemetry.javaagent:opentelemetry-javaagent:${otelInstrumentationVersion}:all")
}

fun isolateAgentClasses (jars: Iterable<File>): CopySpec {
  return copySpec {
    jars.forEach {
      from(zipTree(it)) {
        into("inst")
        rename("(^.*)\\.class\$", "\$1.classdata")
      }
    }
  }
}

tasks {
  compileJava {
    options.release.set(8)
  }

  jar {
    enabled = false
  }

  // includes everything except otel agent
  val relocateAndIsolate by registering(ShadowJar::class) {
    dependsOn(":custom:shadowJar")
    dependsOn(":instrumentation:shadowJar")
    dependsOn(":profiler:shadowJar")
    dependsOn(":bootstrap:jar")
    dependsOn(":agent:jar")

    configurations = listOf(relocateLibs)

    from(sourceSets.main.get().output)

    with(isolateAgentClasses(isolateLibs.files))
  }

  // merge otel agent with our agent
  shadowJar {
    archiveClassifier.set("all")

    from(includeAsIs.files)
    from(relocateAndIsolate.get().outputs)

    manifest {
      attributes(mapOf(
          "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Agent-Class" to "com.splunk.opentelemetry.SplunkAgent",
          "Premain-Class" to "com.splunk.opentelemetry.SplunkAgent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true,
          "Implementation-Vendor" to "Splunk",
          "Implementation-Version" to "splunk-${project.version}-otel-${otelInstrumentationVersion}"
      ))
    }

    mergeServiceFiles {
      include("inst/META-INF/services/*")
    }
  }

  val mainShadowJar by registering(Jar::class) {
    archiveClassifier.set("")

    from(zipTree(shadowJar.get().archiveFile))

    manifest {
      attributes(shadowJar.get().manifest.attributes)
    }
  }

  assemble {
    dependsOn(shadowJar)
    dependsOn(mainShadowJar)
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
