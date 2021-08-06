plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("org.apache.tomcat:tomcat-catalina:9.0.40")

  testImplementation("org.apache.tomcat.embed:tomcat-embed-core:10.0.10")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
