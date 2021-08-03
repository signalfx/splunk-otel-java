plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("com.zaxxer:HikariCP:3.0.0")

  testImplementation("com.zaxxer:HikariCP:3.0.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
