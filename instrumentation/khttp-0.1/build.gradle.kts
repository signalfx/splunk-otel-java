plugins {
  id("groovy")
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

repositories {
  maven {
    url = uri("https://jitpack.io")
  }
}

muzzle {
  pass {
    group.set("com.github.jkcclemens")
    module.set("khttp")
    versions.set("(,)")
  }
}

dependencies {
  compileOnly("com.github.jkcclemens:khttp:0.1.0")

  testImplementation("com.github.jkcclemens:khttp:0.1.0")

  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
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
