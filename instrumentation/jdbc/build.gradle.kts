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

  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("org.testcontainers:testcontainers")

  // SQL Server
  testLibrary("com.microsoft.sqlserver:mssql-jdbc:6.1.0.jre8")
  testImplementation("org.testcontainers:mssqlserver")

  // Oracle
  testLibrary("com.oracle.database.jdbc:ojdbc8:23.9.0.25.07")
  testImplementation("org.testcontainers:oracle-free")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.splunk-jdbc.enabled=true")
}
