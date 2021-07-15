plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  // in tomcat 8.5.28 jmx.ConnectionPool implements MBeanRegistration, which makes instrumentation much simpler
  compileOnly("org.apache.tomcat:tomcat-jdbc:8.5.28")

  testImplementation("org.apache.tomcat:tomcat-jdbc:8.5.28")
  testImplementation("org.awaitility:awaitility")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
