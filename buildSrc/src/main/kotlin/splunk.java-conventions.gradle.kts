plugins {
  java
}

repositories {
  mavenCentral()
  maven {
    name = "sonatypeReleases"
    url = uri("https://oss.sonatype.org/content/repositories/releases/")
  }
  maven {
    name = "sonatypeSnapshots"
    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
  }
}
evaluationDependsOn(":dependencyManagement")
val dependencyManagementConf = configurations.create("dependencyManagement") {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}
afterEvaluate {
  configurations.configureEach {
    if (isCanBeResolved && !isCanBeConsumed) {
      extendsFrom(dependencyManagementConf)
    }
  }
}

dependencies {
  add(dependencyManagementConf.name, platform(project(":dependencyManagement")))
  add("testImplementation", "org.assertj:assertj-core")
  add("testImplementation", "org.awaitility:awaitility")
  add("testImplementation", "org.mockito:mockito-core")
  add("testImplementation", "org.mockito:mockito-junit-jupiter")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-params")
  add("testImplementation", "org.junit.jupiter:junit-jupiter-engine")
  add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
  add("testRuntimeOnly", "org.slf4j:slf4j-api")
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
