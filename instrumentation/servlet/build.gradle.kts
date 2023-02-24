plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("javax.servlet")
    module.set("servlet-api")
    versions.set("[2.3,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("javax.servlet:servlet-api:2.3")

  implementation(project(":instrumentation:common"))

  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common")
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2")
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")

  // Use servlet 2.3 to test Filters too
  testImplementation("javax.servlet:servlet-api:2.3")
  testImplementation("org.eclipse.jetty:jetty-servlet:7.0.0.v20091005")
  testImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
}
