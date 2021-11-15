plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":instrumentation:micrometer-1.3-shaded-for-instrumenting", configuration = "shadow"))
}
