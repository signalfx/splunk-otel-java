plugins {
  java
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
  }
}

dependencies {
  add("testImplementation", "org.assertj:assertj-core")
  add("testImplementation", "org.awaitility:awaitility")
  add("testImplementation", "org.mockito:mockito-core")
  add("testImplementation", "org.mockito:mockito-junit-jupiter")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
  add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
  add("testRuntimeOnly", "org.slf4j:slf4j-api:2.0.6")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  reports {
    junitXml.isOutputPerTestCase = true
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.isDeprecation = true
}
