import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("splunk.shadow-conventions")
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

dependencies {
  // include micrometer-core API
  bootstrapLibs(project(":bootstrap"))
  // include testing extensions, e.g. micrometer
  javaagentLibs(project(":testing:agent-test-extension"))

  // and finally include everything from otel agent for testing
  //TODO remove this `@jar` when upstream sorts its publishing
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-agent-for-testing@jar")
}

tasks {
  jar {
    enabled = false
  }

  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

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

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
      attributes(mapOf(
          "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Agent-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Premain-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true,
      ))
    }
  }

  assemble {
    dependsOn(shadowJar)
  }
}

fun CopySpec.isolateClasses(jars: Iterable<File>) {
  jars.forEach {
    from(zipTree(it)) {
      into("inst")
      rename("(^.*)\\.class\$", "\$1.classdata")
    }
  }
}
