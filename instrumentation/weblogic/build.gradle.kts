plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("javax.servlet")
    module.set("servlet-api")
    versions.set("[2.2,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("javax.servlet:servlet-api:2.2")
  implementation(project(":instrumentation:common"))
}
