import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
  testCompileOnly("com.google.auto.value:auto-value-annotations")
  testAnnotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":profiler"))
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("com.google.protobuf:protobuf-java-util")
  testImplementation("com.squareup.okhttp3:okhttp")
  testImplementation("io.opentelemetry.proto:opentelemetry-proto")
  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("ch.qos.logback:logback-classic:1.5.31")
  testImplementation("com.github.docker-java:docker-java-core")
  testImplementation("com.github.docker-java:docker-java-transport-httpclient5")
  testImplementation("org.mock-server:mockserver-client-java:5.15.0")
}

tasks {
  test {
    testLogging.showStandardStreams = true

    // Run smoke tests only when explicitly requested.
    enabled = enabled && gradle.startParameter.taskNames.any { it.startsWith(":smoke-tests:") }

    develocity.testRetry {
      if (System.getenv().containsKey("CI")) {
        // You can see tests that were retried by this mechanism in the collected test reports and build scans.
        maxRetries.set(5)
      } else {
        maxRetries.set(0)
      }
    }

    val suites = mapOf(
      "glassfish" to listOf("**/GlassFishSmokeTest.*"),
      "jboss" to listOf("**/JBossEapSmokeTest.*"),
      "jetty" to listOf("**/JettySmokeTest.*"),
      "liberty" to listOf("**/LibertySmokeTest.*"),
      "profiler" to listOf("**/Profiler*", "**/SnapshotProfiler*"),
      "tomcat" to listOf("**/TomcatSmokeTest.*"),
      "tomee" to listOf("**/TomeeSmokeTest.*"),
      "weblogic" to listOf("**/WebLogicSmokeTest.*"),
      "websphere" to listOf("**/WebSphereSmokeTest.*"),
      "wildfly" to listOf("**/WildFlySmokeTest.*"),
    )

    val smokeTestSuite: String? by project
    if (smokeTestSuite != null) {
      when {
        "other" == smokeTestSuite -> {
          suites.values.forEach {
            exclude(it)
          }
        }

        suites.containsKey(smokeTestSuite) -> {
          include(suites.getValue(smokeTestSuite!!))
        }

        else -> {
          throw GradleException("Unknown smoke test suite: $smokeTestSuite")
        }
      }
    }

    val shadowTask = project(":agent").tasks.named<ShadowJar>("shadowJar").get()
    inputs.files(layout.files(shadowTask))

    doFirst {
      jvmArgs("-Dio.opentelemetry.smoketest.agent.shadowJar.path=${shadowTask.archiveFile.get().asFile.absolutePath}")
    }
  }
}
