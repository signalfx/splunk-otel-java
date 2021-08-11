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