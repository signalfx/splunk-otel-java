plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("com.zaxxer")
    module.set("HikariCP")
    versions.set("[3.0.0,)")
    // muzzle does not detect PoolStats method references used - some of these methods were introduced in 3.0 and we can't assertInverse
  }
}

dependencies {
  compileOnly("com.zaxxer:HikariCP:3.0.0")

  testImplementation("com.zaxxer:HikariCP:3.0.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")

    // FIXME: temporary change to make debugging hikari tests on CI easier
    testLogging.showStandardStreams = true
  }
}
