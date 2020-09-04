plugins {
  java
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

  testImplementation("org.testcontainers:testcontainers:1.14.3")
  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
  testImplementation("com.google.protobuf:protobuf-java-util:3.12.4")
  testImplementation("com.squareup.okhttp3:okhttp:3.12.12")
  testImplementation("io.opentelemetry:opentelemetry-proto:0.8.0")

  testImplementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.test {
  useJUnitPlatform()
  dependsOn(":agent:shadowJar")

  doFirst {
    val shadowTask : Jar = project(":agent").tasks.named<Jar>("shadowJar").get()
    jvmArgs("-Dio.opentelemetry.smoketest.agent.shadowJar.path=${shadowTask.archiveFile.get()}")
  }
}