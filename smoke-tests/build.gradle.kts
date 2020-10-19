plugins {
  java
}

dependencies {
  testImplementation("org.testcontainers:testcontainers:1.15.0-rc2")
  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
  testImplementation("com.google.protobuf:protobuf-java-util:3.12.4")
  testImplementation("com.squareup.okhttp3:okhttp:3.12.12")
  testImplementation("io.opentelemetry:opentelemetry-proto:0.9.1")

  testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.test {
  useJUnitPlatform()
  reports {
    junitXml.isOutputPerTestCase = true
  }

  val shadowTask : Jar = project(":agent").tasks.named<Jar>("shadowJar").get()
  inputs.files(layout.files(shadowTask))

  doFirst {
    jvmArgs("-Dio.opentelemetry.smoketest.agent.shadowJar.path=${shadowTask.archiveFile.get()}")
  }
}