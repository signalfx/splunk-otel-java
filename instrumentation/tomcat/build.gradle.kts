plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.apache.tomcat")
    module.set("tomcat-catalina")
    versions.set("[8,)")
    // no assertInverse because metrics and attributes instrumentations support different version ranges
  }
}

dependencies {
  compileOnly("org.apache.tomcat:tomcat-catalina:9.0.40")
  implementation(project(":instrumentation:common"))

  testImplementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.40")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
    jvmArgs("-Dsplunk.metrics.otel.enabled=true")
  }
}
