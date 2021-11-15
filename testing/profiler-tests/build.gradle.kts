import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("java")
}

dependencies {
  testImplementation(project(":profiler"))
  testImplementation("org.slf4j:slf4j-simple")
  testImplementation("io.opentelemetry.proto:opentelemetry-proto")
  testImplementation("com.google.protobuf:protobuf-java-util")
  testImplementation("org.testcontainers:testcontainers")
}

tasks {
  jar {
    enabled = false
  }

  test {
    val shadowTask = project(":agent").tasks.named<ShadowJar>("shadowJar").get()
    inputs.files(layout.files(shadowTask))

    doFirst {
      jvmArgs("-Dio.opentelemetry.smoketest.agent.shadowJar.path=${shadowTask.archiveFile.get()}")
    }
  }
}
