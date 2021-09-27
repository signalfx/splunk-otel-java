// a separate, optional muzzle conventions file is used because some projects (tomee) fail with a mysterious
// "java.util.zip.ZipException: zip END header not found" error when muzzle is turned on everywhere by default

plugins {
  id("splunk.java-conventions")

  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("io.opentelemetry.instrumentation.muzzle-check")
}

dependencies {
  // dependencies needed to make muzzle-check work
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  add("muzzleTooling", "ch.qos.logback:logback-classic:1.2.5")
  add("muzzleBootstrap", project(":bootstrap"))
  add("muzzleBootstrap", "io.opentelemetry:opentelemetry-api")
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-muzzle")
}
