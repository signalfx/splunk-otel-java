plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.apache.tomcat")
    module.set("tomcat-catalina")
    versions.set("[7,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.apache.tomcat:tomcat-catalina:9.0.40")
}
