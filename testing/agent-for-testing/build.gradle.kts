import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("splunk.shadow-conventions")
}

// dependencies that already are relocated and will be moved to inst/ (agent classloader isolation)
val isolateLibs by configurations.creating
// dependencies that will be relocated
val relocateLibs by configurations.creating
// dependencies that will be included as they are
val includeAsIs by configurations.creating

dependencies {
  // include micrometer-core API
  relocateLibs(project(":bootstrap"))
  // include testing extensions, e.g. micrometer
  isolateLibs(project(":testing:agent-test-extension", configuration = "shadow"))

  // and finally include everything from otel agent for testing
  //TODO remove this `@jar` when upstream sorts its publishing
  includeAsIs("io.opentelemetry.javaagent:opentelemetry-agent-for-testing@jar")
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
  jar {
    enabled = false
  }

  val relocateAndIsolate by registering(ShadowJar::class) {
    dependsOn(":testing:agent-test-extension:shadowJar")

    configurations = listOf(relocateLibs)

    with(isolateAgentClasses(isolateLibs.files))
  }

  shadowJar {
    from(includeAsIs.files)
    from(relocateAndIsolate.get().outputs)

    manifest {
      attributes(mapOf(
          "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Agent-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Premain-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true,
      ))
    }

    mergeServiceFiles {
      include("inst/META-INF/services/**")
    }
  }

  assemble {
    dependsOn(shadowJar)
  }
}