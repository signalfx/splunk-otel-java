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

    // 4.0.0 uses a broken slf4j version: the "${slf4j.version}" placeholder is taken literally
    skip("4.0.0")
  }
}

dependencies {
  compileOnly("com.zaxxer:HikariCP:3.0.0")
  implementation(project(":instrumentation:common"))

  testImplementation("com.zaxxer:HikariCP:3.0.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
