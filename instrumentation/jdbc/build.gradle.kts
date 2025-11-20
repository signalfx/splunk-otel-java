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
  compileOnly(project(":custom"))

  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-jdbc")

  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("org.testcontainers:testcontainers")

  // SQL Server
  testLibrary("com.microsoft.sqlserver:mssql-jdbc:6.1.0.jre8")
  testImplementation("org.testcontainers:testcontainers-mssqlserver")

  // Oracle
  testLibrary("com.oracle.database.jdbc:ojdbc8:23.9.0.25.07")
  testImplementation("org.testcontainers:testcontainers-oracle-free")

  // PostgreSQL
  testLibrary("org.postgresql:postgresql:42.1.1")
  testImplementation("org.testcontainers:testcontainers-postgresql")
}

tasks.withType<Test>().configureEach {
  systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
  jvmArgs("-Dotel.instrumentation.splunk-jdbc.enabled=true")
  jvmArgs("-Dotel.service.name=test-service")
}
