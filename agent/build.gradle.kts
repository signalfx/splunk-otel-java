plugins {
  java
  id("com.github.johnrengelman.shadow") version "6.0.0"
}

val versions: Map<String, String> by extra

dependencies {
  implementation("io.opentelemetry.javaagent", "opentelemetry-javaagent", version = versions["opentelemetryJavaagent"], classifier = "all")
}

base.archivesBaseName = "splunk-otel-javaagent"

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
      attributes.put("Agent-Class", "com.splunk.opentelemetry.SplunkAgent")
      attributes.put("Premain-Class", "com.splunk.opentelemetry.SplunkAgent")
      attributes.put("Can-Redefine-Classes", "true")
      attributes.put("Can-Retransform-Classes", "true")
      attributes.put("Implementation-Vendor", "Splunk")
      attributes.put("Implementation-Version", "splunk-${project.version}-otel-${versions["opentelemetryJavaagent"]}")
    }
  }

  assemble {
    dependsOn(shadowJar)
  }
}