plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":instrumentation:compile-stub"))
}