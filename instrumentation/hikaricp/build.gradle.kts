plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("com.zaxxer:HikariCP:4.0.3")

  testImplementation("com.zaxxer:HikariCP:3.0.0")
  testImplementation("org.awaitility:awaitility")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
