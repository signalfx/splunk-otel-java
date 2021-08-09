plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.wildfly.core")
    module.set("wildfly-version")
    versions.set("(,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.wildfly.core:wildfly-version:13.0.0.Final")
}