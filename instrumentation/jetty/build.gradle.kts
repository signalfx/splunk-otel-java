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
  compileOnly("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
}
