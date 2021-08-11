plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.eclipse.jetty")
    module.set("jetty-server")
    versions.set("(,)")
  }
}

dependencies {
  compileOnly("org.eclipse.jetty:jetty-server:9.4.35.v20201120")
}