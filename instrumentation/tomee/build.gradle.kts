plugins {
  id("splunk.instrumentation-conventions")
  // TODO: make this module work with muzzle
}

dependencies {
  compileOnly("org.apache.tomee:openejb-core:8.0.6")
}