/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("splunk.java-conventions")
  id("splunk.shadow-conventions")
}

val testInstrumentation by configurations.creating

dependencies {
  add("testInstrumentation", platform(project(":dependencyManagement")))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")
  compileOnly(project(":bootstrap"))

  // test
  testImplementation(project(":testing:common"))
  // the bootstrap module is provided by the javaagent in the instrumentation test runtime, no need to include it
  // (especially when it's not being shaded)
  testCompileOnly(project(":bootstrap"))
}

tasks.named<ShadowJar>("shadowJar").configure {
  configurations = listOf(project.configurations.runtimeClasspath.get(), testInstrumentation)

  archiveFileName.set("agent-testing.jar")
}

evaluationDependsOn(":testing:agent-for-testing")

tasks.withType<Test>().configureEach {
  val shadowJar = tasks.shadowJar.get()
  val agentShadowJar = project(":testing:agent-for-testing").tasks.shadowJar

  inputs.file(shadowJar.archiveFile)

  dependsOn(shadowJar)
  dependsOn(agentShadowJar.get())

  jvmArgumentProviders.add(JavaagentProvider(agentShadowJar.flatMap { it.archiveFile }))

  jvmArgs("-Dotel.javaagent.debug=true")
  jvmArgs("-Dotel.javaagent.experimental.initializer.jar=${shadowJar.archiveFile.get().asFile.absolutePath}")
  jvmArgs("-Dotel.javaagent.testing.additional-library-ignores.enabled=false")
  jvmArgs("-Dotel.javaagent.testing.fail-on-context-leak=true")
  // prevent sporadic gradle deadlocks, see SafeLogger for more details
  jvmArgs("-Dotel.javaagent.testing.transform-safe-logging.enabled=true")
  // Reduce noise in assertion messages since we don't need to verify this in most tests. We check
  // in smoke tests instead.
  jvmArgs("-Dotel.javaagent.add-thread-details=false")
  // suppress repeated logging of "No metric data to export - skipping export."
  // since PeriodicMetricReader is configured with a short interval
  jvmArgs("-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.opentelemetry.sdk.metrics.export.PeriodicMetricReader=INFO")

  val trustStore = project(":testing:common").file("src/misc/testing-keystore.p12")
  inputs.file(trustStore)
  jvmArgs("-Djavax.net.ssl.trustStore=${trustStore.absolutePath}")
  jvmArgs("-Djavax.net.ssl.trustStorePassword=testing")

  // The sources are packaged into the testing jar so we need to make sure to exclude from the test
  // classpath, which automatically inherits them, to ensure our shaded versions are used.
  classpath = classpath.filter {
    if (layout.buildDirectory.file("resources/main") == it || layout.buildDirectory.file("classes/java/main") == it) {
      return@filter false
    }
    return@filter true
  }
}

/**
 * We define three dependency configurations to use when adding dependencies to libraries being
 * instrumented.
 *
 * - library: A dependency on the instrumented library. Results in the dependency being added to
 *     compileOnly and testImplementation. If the build is run with -PtestLatestDeps=true, the
 *     version when added to testImplementation will be overridden by `+`, the latest version
 *     possible. For simple libraries without different behavior between versions, it is possible
 *     to have a single dependency on library only.
 *
 * - testLibrary: A dependency on a library for testing. This will usually be used to either
 *     a) use a different version of the library for compilation and testing and b) to add a helper
 *     that is only required for tests (e.g., library-testing artifact). The dependency will be
 *     added to testImplementation and will have a version of `+` when testing latest deps as
 *     described above.
 *
 * - latestDepTestLibrary: A dependency on a library for testing when testing of latest dependency
 *   version is enabled. This dependency will be added as-is to testImplementation, but only if
 *   -PtestLatestDeps=true. The version will not be modified but it will be given highest
 *   precedence. Use this to restrict the latest version dependency from the default `+`, for
 *   example to restrict to just a major version by specifying `2.+`.
 */

val testLatestDeps = gradle.startParameter.projectProperties["testLatestDeps"] == "true"
extra["testLatestDeps"] = testLatestDeps

@CacheableRule
abstract class TestLatestDepsRule : ComponentMetadataRule {
  override fun execute(context: ComponentMetadataContext) {
    val version = context.details.id.version
    if (version.contains("-alpha", true)
      || version.contains("-beta", true)
      || version.contains("-rc", true)
      || version.contains(".rc", true)
      || version.contains("-m", true) // e.g. spring milestones are published to grails repo
      || version.contains(".m", true) // e.g. lettuce
      || version.contains(".alpha", true) // e.g. netty
      || version.contains(".beta", true) // e.g. hibernate
      || version.contains(".cr", true) // e.g. hibernate
      || version.endsWith("-nf-execution") // graphql
      || GIT_SHA_PATTERN.matches(version) // graphql
      || DATETIME_PATTERN.matches(version) // graphql
    ) {
      context.details.status = "milestone"
    }
  }

  companion object {
    private val GIT_SHA_PATTERN = Regex("^.*-[0-9a-f]{7,}$")
    private val DATETIME_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}.*$")
  }
}

configurations {
  val library by creating {
    isCanBeResolved = false
    isCanBeConsumed = false
  }
  val testLibrary by creating {
    isCanBeResolved = false
    isCanBeConsumed = false
  }
  val latestDepTestLibrary by creating {
    isCanBeResolved = false
    isCanBeConsumed = false
  }

  val testImplementation by getting

  listOf(library, testLibrary).forEach { configuration ->
    // We use whenObjectAdded and copy into the real configurations instead of extension to allow
    // mutating the version for latest dep tests.
    configuration.dependencies.whenObjectAdded {
      val dep = copy()
      if (testLatestDeps) {
        (dep as ExternalDependency).version {
          require("latest.release")
        }
      }
      testImplementation.dependencies.add(dep)
    }
  }
  if (testLatestDeps) {
    dependencies {
      components {
        all<TestLatestDepsRule>()
      }
    }

    latestDepTestLibrary.dependencies.whenObjectAdded {
      val dep = copy()
      val declaredVersion = dep.version
      if (declaredVersion != null) {
        (dep as ExternalDependency).version {
          strictly(declaredVersion)
        }
      }
      testImplementation.dependencies.add(dep)
    }
  }
  named("compileOnly") {
    extendsFrom(library)
  }
}

if (testLatestDeps) {
  afterEvaluate {
    tasks {
      withType<JavaCompile>().configureEach {
        with(options) {
          // We may use methods that are deprecated in future versions, we check lint on the normal
          // build and don't need this for testLatestDeps.
          compilerArgs.add("-Xlint:-deprecation")
        }
      }
    }

    if (tasks.names.contains("latestDepTest")) {
      val latestDepTest by tasks.existing
      tasks.named("test").configure {
        dependsOn(latestDepTest)
      }
    }
  }
} else {
  afterEvaluate {
    // Disable compiling latest dep tests for non latest dep builds in CI. This is needed to avoid
    // breaking build because of a new library version which could force backporting latest dep
    // fixes to release branches.
    // This is only needed for modules where base version and latest dep tests use a different
    // source directory.
    var latestDepCompileTaskNames = arrayOf("compileLatestDepTestJava", "compileLatestDepTestGroovy", "compileLatestDepTestScala")
    for (compileTaskName in latestDepCompileTaskNames) {
      if (tasks.names.contains(compileTaskName)) {
        tasks.named(compileTaskName).configure {
          enabled = false
        }
      }
    }
  }
}

tasks {
  register("generateInstrumentationVersionFile") {
    val name = "com.splunk.${project.name}"
    val version = project.version.toString()
    inputs.property("instrumentation.name", name)
    inputs.property("instrumentation.version", version)

    val propertiesDir = layout.buildDirectory.dir("generated/instrumentationVersion/META-INF/io/opentelemetry/instrumentation")
    outputs.dir(propertiesDir)

    doLast {
      File(propertiesDir.get().asFile, "$name.properties").writeText("version=$version")
    }
  }
}

sourceSets {
  main {
    output.dir("build/generated/instrumentationVersion", "builtBy" to "generateInstrumentationVersionFile")
  }
}

class JavaagentProvider(
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val agentJar: Provider<RegularFile>,
) : CommandLineArgumentProvider {
  override fun asArguments(): Iterable<String> = listOf(
    "-javaagent:${file(agentJar).absolutePath}",
  )
}
