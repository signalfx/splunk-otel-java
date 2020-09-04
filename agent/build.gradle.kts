plugins {
  java
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

dependencies {
  implementation("io.opentelemetry.javaagent", "opentelemetry-javaagent", version = "0.8.0-20200904.061307-97", classifier = "all")
}

tasks {
  compileJava {
    options.release.set(8)
  }

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
      attributes.put("Main-Class", "io.opentelemetry.javaagent.OpenTelemetryAgent")
      attributes.put("Agent-Class", "com.signalfx.opentelemetry.SplunkAgent")
      attributes.put("Premain-Class", "com.signalfx.opentelemetry.SplunkAgent")
      attributes.put("Can-Redefine-Classes", "true")
      attributes.put("Can-Retransform-Classes", "true")
      attributes.put("Implementation-Vendor", "Splunk")
      attributes.put("Implementation-Version", project.version)
    }
  }
}