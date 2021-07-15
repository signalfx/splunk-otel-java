plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("org.apache.tomcat:tomcat-jdbc:8.5.0")

  testImplementation("org.apache.tomcat:tomcat-jdbc:8.5.0")
  testImplementation("org.awaitility:awaitility")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
