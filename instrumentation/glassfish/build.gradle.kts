plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.glassfish.main.common")
    module.set("common-util")
    versions.set("[5.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.glassfish.main.common:common-util:5.0")
}