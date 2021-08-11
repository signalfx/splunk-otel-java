plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.vibur")
    module.set("vibur-dbcp")
    versions.set("[11.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.vibur:vibur-dbcp:11.0")

  testImplementation("org.vibur:vibur-dbcp:11.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
