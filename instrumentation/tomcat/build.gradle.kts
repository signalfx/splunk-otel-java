plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("org.apache.tomcat:tomcat-catalina:9.0.40")

  testImplementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.40")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
