plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("com.microsoft.sqlserver")
    module.set("mssql-jdbc")
    versions.set("(,)")
  }
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-api")

  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jdbc")

  testLibrary("com.microsoft.sqlserver:mssql-jdbc:6.1.0.jre8")
  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:mssqlserver")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.splunk-jdbc.enabled=true")
}
