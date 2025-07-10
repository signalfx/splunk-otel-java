plugins {
  id("groovy")
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.danilopianini")
    module.set("khttp")
    versions.set("(,)")
  }
}

dependencies {
  library("org.danilopianini:khttp:1.1.0")
}

tasks.withType<Test>().configureEach {
  // required on jdk17
  jvmArgs("--add-opens=java.base/java.net=ALL-UNNAMED")
  jvmArgs("--add-opens=java.base/sun.net.www.protocol.https=ALL-UNNAMED")
  jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")

  // override the default always_on sampler because http client test suite includes a test that
  // fails with it
  jvmArgs("-Dotel.traces.sampler=parentbased_always_on")
}
