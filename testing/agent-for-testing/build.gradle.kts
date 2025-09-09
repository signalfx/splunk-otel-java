import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("splunk.shadow-conventions")
}

// this configuration collects libs that will be placed in the bootstrap classloader
val bootstrapLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration collects libs that will be placed in the agent classloader, isolated from the instrumented application code
val javaagentLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration stores the upstream agent dep that's extended by this project
val upstreamAgent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  add("bootstrapLibs", platform(project(":dependencyManagement")))
  add("javaagentLibs", platform(project(":dependencyManagement")))
  add("upstreamAgent", platform(project(":dependencyManagement")))

  bootstrapLibs(project(":bootstrap"))
  javaagentLibs(project(":custom"))
  // include testing extensions
  javaagentLibs(project(":testing:agent-test-extension"))

  // and finally include everything from otel agent for testing
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-agent-for-testing")
}

tasks {
  jar {
    enabled = false
  }

  // building the final javaagent jar is done in 3 steps:

  // 1. all Splunk-specific javaagent libs are relocated (by the splunk.shadow-conventions plugin)
  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

    archiveFileName.set("javaagentLibs-relocated.jar")

    // exclude known bootstrap dependencies - they can't appear in the inst/ directory
    dependencies {
      exclude(dependency("org.slf4j:slf4j-api"))
      exclude(dependency("io.opentelemetry:opentelemetry-api"))
      exclude(dependency("io.opentelemetry:opentelemetry-common"))
      exclude(dependency("io.opentelemetry:opentelemetry-context"))
      exclude(dependency("io.opentelemetry.semconv:opentelemetry-semconv"))
      exclude(dependency("io.opentelemetry.semconv:opentelemetry-semconv-incubating"))
      // events API and metrics advice API
      exclude(dependency("io.opentelemetry:opentelemetry-api-incubator"))
    }
  }

  // 2. the Splunk javaagent libs are then isolated - moved to the inst/ directory
  // having a separate task for isolating javaagent libs is required to avoid duplicates with the upstream javaagent
  // duplicatesStrategy in shadowJar won't be applied when adding files with with(CopySpec) because each CopySpec has
  // its own duplicatesStrategy
  val isolateJavaagentLibs by registering(Copy::class) {
    dependsOn(relocateJavaagentLibs)
    isolateClasses(relocateJavaagentLibs.get().outputs.files)

    into(layout.buildDirectory.dir("isolated/javaagentLibs"))
  }

  // 3. the relocated and isolated javaagent libs are merged together with the bootstrap libs (which undergo relocation
  // in this task) and the upstream javaagent jar; duplicates are removed
  shadowJar {
    configurations = listOf(bootstrapLibs, upstreamAgent)

    dependsOn(isolateJavaagentLibs)
    from(isolateJavaagentLibs.get().outputs)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles("inst/META-INF/services")
    // mergeServiceFiles requires that duplicate strategy is set to include
    filesMatching("inst/META-INF/services/**") {
      duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    manifest {
      attributes(
        mapOf(
          "Main-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Agent-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Premain-Class" to "io.opentelemetry.javaagent.OpenTelemetryAgent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true,
        ),
      )
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
      rename("^(.*)\\.class\$", "\$1.classdata")
    }
  }
}
