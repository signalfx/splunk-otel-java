import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer

plugins {
  id("maven-publish")
  id("signing")
  id("splunk.shadow-conventions")
  id("com.github.jk1.dependency-license-report")
}

base.archivesName.set("splunk-otel-javaagent")

java {
  withJavadocJar()
  withSourcesJar()
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

val licenseReportDependencies: Configuration by configurations.creating {
  extendsFrom(bootstrapLibs)
  extendsFrom(javaagentLibs)
}

val otelInstrumentationVersion: String by rootProject.extra

dependencies {
  add("upstreamAgent", platform(project(":dependencyManagement")))
  bootstrapLibs(project(":bootstrap"))

  javaagentLibs(project(":custom"))
  javaagentLibs(project(":profiler"))

  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent")
}

val javaagentDependencies = dependencies

// collect all instrumentation sub projects
project(":instrumentation").subprojects {
  val subProj = this
  plugins.withId("splunk.instrumentation-conventions") {
    javaagentDependencies.run {
      add(javaagentLibs.name, project(subProj.path))
    }
  }
}

tasks {
  jar {
    enabled = false
  }

  processResources {
    from(rootProject.file("licenses")) {
      into("META-INF/licenses")
    }
  }

  // building the final javaagent jar is done in 3 steps:

  // 1. all Splunk-specific javaagent libs are relocated (by the splunk.shadow-conventions plugin)
  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

    duplicatesStrategy = DuplicatesStrategy.FAIL

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

    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("LICENSE.txt")
    exclude("THIRD_PARTY_LICENSES.txt")
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
    filesMatching("META-INF/licenses/licenses.md") {
      // rename our licenses report to avoid overwriting the one from upstream
      this.path = this.path.replace("licenses.md", "licenses-splunk-otel-java.md")
    }

    archiveClassifier.set("all")

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
          "Agent-Class" to "com.splunk.opentelemetry.javaagent.SplunkAgent",
          "Premain-Class" to "com.splunk.opentelemetry.javaagent.SplunkAgent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true,
          "Implementation-Vendor" to "Splunk",
          "Implementation-Version" to "splunk-${project.version}-otel-$otelInstrumentationVersion",
        ),
      )
    }
  }

  // a separate task to create a no-classifier jar that's exactly the same as the -all one
  // because a no-classifier (main) jar is required by sonatype
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

  val cleanLicenses by registering(Delete::class) {
    delete(rootProject.file("licenses"))
  }

  val generateLicenseReportEnabled = gradle.startParameter.taskNames.any { it.equals("generateLicenseReport") }
  named("generateLicenseReport").configure {
    dependsOn(cleanLicenses)
    // disable licence report generation unless this task is explicitly run
    // the files produced by this task are used by other tasks without declaring them as dependency
    // which gradle considers an error
    enabled = enabled && generateLicenseReportEnabled
  }
}

licenseReport {
  outputDir = rootProject.file("licenses").absolutePath

  renderers = arrayOf(InventoryMarkdownReportRenderer(
    "licenses.md",
    "splunk-otel-javaagent",
    File("$projectDir/license-overrides.txt")
  ))

  configurations = arrayOf(licenseReportDependencies.name)

  excludeBoms = true

  excludeGroups = arrayOf(
    "splunk-otel-java\\.instrumentation",
  )

  excludes = arrayOf(
    "io.opentelemetry:opentelemetry-bom-alpha",
  )

  filters = arrayOf(LicenseBundleNormalizer("$projectDir/license-normalizer-bundle.json", true))
}

fun CopySpec.isolateClasses(jars: Iterable<File>) {
  jars.forEach {
    from(zipTree(it)) {
      into("inst")
      rename("^(.*)\\.class\$", "\$1.classdata")
      exclude("META-INF/LICENSE")
      exclude("META-INF/INDEX.LIST")
      exclude("META-INF/*.DSA")
      exclude("META-INF/*.SF")
    }
  }
}
