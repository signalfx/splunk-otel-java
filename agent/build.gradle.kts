plugins {
  java
  id("com.github.johnrengelman.shadow") version "5.2.0"
}

//val customisation by configurations.creating

dependencies {
  implementation("io.opentelemetry.instrumentation.auto", "opentelemetry-javaagent", version = "0.7.0", classifier = "all")

//  customisation(project(":custom"), classifier = "all")
}

tasks {
  processResources {
    val customizationShadowTask = project(":custom").tasks.named<Jar>("shadowJar")
    val providerArchive = customizationShadowTask.get().archiveFile
    from(zipTree(providerArchive)) {
//    sourceTasks.collect { zipTree(it.archiveFile) }
//    from(customisation.files.map { zipTree(it) }) {
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

//tasks {
//  shadowJar {
//    mergeServiceFiles {
//      include("inst/META-INF/services/*")
//    }
//
//    // Prevents conflict with other SLF4J instances. Important for premain.
//    relocate("org.slf4j", "io.opentelemetry.auto.slf4j")
//    // rewrite dependencies calling Logger.getLogger
//    relocate("java.util.logging.Logger", "io.opentelemetry.auto.bootstrap.PatchLogger")
//
//    // relocate OpenTelemetry API
//    relocate("io.opentelemetry.OpenTelemetry", "io.opentelemetry.auto.shaded.io.opentelemetry.OpenTelemetry")
//    relocate("io.opentelemetry.common", "io.opentelemetry.auto.shaded.io.opentelemetry.common")
//    relocate("io.opentelemetry.context", "io.opentelemetry.auto.shaded.io.opentelemetry.context")
//    relocate("io.opentelemetry.correlationcontext", "io.opentelemetry.auto.shaded.io.opentelemetry.correlationcontext")
//    relocate("io.opentelemetry.internal", "io.opentelemetry.auto.shaded.io.opentelemetry.internal")
//    relocate("io.opentelemetry.metrics", "io.opentelemetry.auto.shaded.io.opentelemetry.metrics")
//    relocate("io.opentelemetry.trace", "io.opentelemetry.auto.shaded.io.opentelemetry.trace")
//    relocate("io.opentelemetry.contrib.auto.annotations", "io.opentelemetry.auto.shaded.io.opentelemetry.contrib.auto.annotations")
//
//    // relocate OpenTelemetry API dependency
////    relocate("io.grpc", "io.opentelemetry.auto.shaded.io.grpc")
//
////    exclude("com/signalfx/**/*.class")
////    from(jar.get().archiveFile)
////    configurations.add(project.configurations.runtimeClasspath.get())
////    from("com/signalfx/**/*.class"){
//////      exclude("META-INF/MANIFEST.MF")
//////    exclude("com/signalfx/**/*.class")
////      into("inst")
////      rename("(^.*)\\.class$", "$1.classdata")
////    }
//  }
//}