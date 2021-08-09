plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

dependencies {
  compileOnly(project(":instrumentation:compile-stub"))
}