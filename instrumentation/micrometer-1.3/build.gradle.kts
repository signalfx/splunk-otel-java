plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

// it's not really possible to use the muzzle-check plugin here - we're instrumenting a (temporarily) shaded micrometer

dependencies {
  implementation(project(":instrumentation:micrometer-common"))

  compileOnly(project(":instrumentation:micrometer-1.3-shaded-for-instrumenting", configuration = "shadow"))
}
