plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.apache.commons")
    module.set("commons-dbcp2")
    versions.set("[2.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.apache.commons:commons-dbcp2:2.0")

  testImplementation("org.apache.commons:commons-dbcp2:2.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
