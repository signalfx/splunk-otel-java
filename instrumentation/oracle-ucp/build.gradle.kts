plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("com.oracle.database.jdbc")
    module.set("ucp")
    versions.set("[,)")
  }
}

dependencies {
  compileOnly("com.oracle.database.jdbc:ucp:11.2.0.4")
  implementation(project(":instrumentation:common"))

  testImplementation("com.oracle.database.jdbc:ucp:11.2.0.4")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
