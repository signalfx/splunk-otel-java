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
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")
  compileOnly(project(":bootstrap"))
  compileOnly("io.micrometer:micrometer-core")

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
  // needed for proper GlobalMeterProvider registration
  jvmArgs("-Dotel.metrics.exporter=otlp")
  // suppress repeated logging of "No metric data to export - skipping export."
  // since PeriodicMetricReader is configured with a short interval
  jvmArgs("-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.io.opentelemetry.sdk.metrics.export.PeriodicMetricReader=INFO")
  // disable agent micrometer registry, when running tests we'll use the registry provided by TestMicrometerInstaller
  jvmArgs("-Dsplunk.metrics.testing.micrometer.disabled=true")

  val trustStore = project(":testing:common").file("src/misc/testing-keystore.p12")
  inputs.file(trustStore)
  jvmArgs("-Djavax.net.ssl.trustStore=${trustStore.absolutePath}")
  jvmArgs("-Djavax.net.ssl.trustStorePassword=testing")

  // The sources are packaged into the testing jar so we need to make sure to exclude from the test
  // classpath, which automatically inherits them, to ensure our shaded versions are used.
  classpath = classpath.filter {
    if (file("$buildDir/resources/main") == it || file("$buildDir/classes/java/main") == it) {
      return@filter false
    }
    return@filter true
  }
}

tasks {
  register("generateInstrumentationVersionFile") {
    val name = "com.splunk.${project.name}"
    val version = project.version.toString()
    inputs.property("instrumentation.name", name)
    inputs.property("instrumentation.version", version)

    val propertiesDir = File(project.buildDir, "generated/instrumentationVersion/META-INF/io/opentelemetry/instrumentation/")
    outputs.dir(propertiesDir)

    doLast {
      File(propertiesDir, "$name.properties").writeText("version=$version")
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
