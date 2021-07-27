import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("org.gradle.test-retry")
}

dependencies {
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
  testImplementation("com.google.protobuf:protobuf-java-util")
  testImplementation("com.squareup.okhttp3:okhttp")
  testImplementation("io.opentelemetry:opentelemetry-proto")
  testImplementation("io.opentelemetry:opentelemetry-api")

  testImplementation("ch.qos.logback:logback-classic:1.2.5")

  testImplementation("com.github.docker-java:docker-java-core")
  testImplementation("com.github.docker-java:docker-java-transport-httpclient5")
}

tasks {
  test {
    maxParallelForks = 2

    testLogging.showStandardStreams = true

    retry {
      // You can see tests that were retried by this mechanism in the collected test reports and build scans.
      maxRetries.set(if (System.getenv("CI") != null) 5 else 0)
    }

    val suites = mapOf(
        "glassfish" to listOf("**/GlassFishSmokeTest.*"),
        "jboss" to listOf("**/JBossEapSmokeTest.*"),
        "jetty" to listOf("**/JettySmokeTest.*"),
        "liberty" to listOf("**/LibertySmokeTest.*"),
        "tomcat" to listOf("**/TomcatSmokeTest.*"),
        "tomee" to listOf("**/TomeeSmokeTest.*"),
        "weblogic" to listOf("**/WebLogicSmokeTest.*"),
        "wildfly" to listOf("**/WildFlySmokeTest.*")
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