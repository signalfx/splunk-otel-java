plugins {
  java
  id("com.github.johnrengelman.shadow") version "5.2.0"
  id("com.diffplug.spotless") version "5.2.0"
}

apply(from = "$rootDir/gradle/spotless.gradle")

dependencies {
  implementation("io.opentelemetry.instrumentation.auto", "opentelemetry-javaagent", version = "0.7.0", classifier = "all")
}

tasks {
  processResources {
    val customizationShadowTask = project(":custom").tasks.named<Jar>("shadowJar")
    val providerArchive = customizationShadowTask.get().archiveFile
    from(zipTree(providerArchive)) {
      into("inst")
      rename("(^.*)\\.class$", "$1.classdata")
    }
    dependsOn(customizationShadowTask)
  }

  shadowJar {
    mergeServiceFiles {
      include("inst/META-INF/services/*")
    }
    exclude("**/module-info.class")
    manifest {
      attributes.put("Main-Class", "io.opentelemetry.auto.bootstrap.AgentBootstrap")
      attributes.put("Agent-Class", "com.signalfx.opentelemetry.SplunkAgent")
      attributes.put("Premain-Class", "com.signalfx.opentelemetry.SplunkAgent")
      attributes.put("Can-Redefine-Classes", "true")
      attributes.put("Can-Retransform-Classes", "true")
      attributes.put("Implementation-Vendor", "Splunk")
    }
  }
}