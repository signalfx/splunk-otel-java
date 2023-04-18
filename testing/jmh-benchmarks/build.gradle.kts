plugins {
  `java-library`
  id("me.champeau.jmh") version "0.7.1"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":profiler"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

jmh {
  profilers.add("gc")
  warmupIterations.set(2)
  iterations.set(10)
  fork.set(2)
}
