plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("com.mchange")
    module.set("c3p0")
    versions.set("[0.9.2,)")
    assertInverse.set(true)
    // missing deps in maven central
    skip("0.9.2-pre2-RELEASE", "0.9.2-pre3")
  }
}

dependencies {
  compileOnly("com.mchange:c3p0:0.9.2")
  implementation(project(":instrumentation:common"))

  testImplementation("com.mchange:c3p0:0.9.2")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
