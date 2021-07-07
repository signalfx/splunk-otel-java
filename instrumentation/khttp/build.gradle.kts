plugins {
  id("groovy")
  id("splunk.instrumentation-conventions")
}

repositories {
  maven {
    url = uri("https://jitpack.io")
  }
}

dependencies {
  compileOnly("com.github.jkcclemens:khttp:0.1.0")

  testImplementation("com.github.jkcclemens:khttp:0.1.0")

  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}