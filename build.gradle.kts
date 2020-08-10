plugins {
  java
//  id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "com.signalfx.public"
version = "1.0-SNAPSHOT"

subprojects {
  repositories {
    jcenter()
    maven {
      url = uri("https://dl.bintray.com/open-telemetry/maven")
    }
    mavenCentral()
  }
}

//dependencies {
//  implementation("io.opentelemetry.instrumentation.auto", "opentelemetry-javaagent", version = "0.7.0", classifier = "all")
//  compileOnly("io.opentelemetry:opentelemetry-sdk:0.7.0")
//  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-otlp:0.7.0")
//  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-jaeger:0.7.0")
//  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-zipkin:0.7.0")
//  compileOnly("io.opentelemetry.instrumentation.auto:opentelemetry-auto-exporter-logging:0.7.0")
//  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-auto-config:0.7.0")
//}

//fun isolateSpec(sourceTasks: Collection<Jar>): CopySpec {
//  return copySpec {
//    from(sourceTasks.map { zipTree(it.archiveFile) }) {
//      // important to keep prefix 'inst' short, as it is prefixed to lots of strings in runtime mem
//      into("inst")
//      rename("(^.*)\\.class$", "$1.classdata")
//    }
//  }
//}


//tasks {
//  jar {
//    into("inst")
//    rename("(^.*)\\.class$", "$1.classdata")
//  }
//  shadowJar {
//    mergeServiceFiles {
//      include("inst/META-INF/services/*")
//    }
//    manifest {
//      attributes.put("Main-Class", "io.opentelemetry.auto.bootstrap.AgentBootstrap")
//      attributes.put("Agent-Class", "com.signalfx.opentelemetry.SplunkAgent")
//      attributes.put("Premain-Class", "com.signalfx.opentelemetry.SplunkAgent")
//      attributes.put("Can-Redefine-Classes", "true")
//      attributes.put("Can-Retransform-Classes", "true")
//      attributes.put("Implementation-Vendor", "Splunk")
//    }
//    exclude("com/signalfx/**/*.class")
//    from(jar.get().archiveFile)
////    configurations.add(project.configurations.runtimeClasspath.get())
////    from("com/signalfx/**/*.class"){
//////      exclude("META-INF/MANIFEST.MF")
//////    exclude("com/signalfx/**/*.class")
////      into("inst")
////      rename("(^.*)\\.class$", "$1.classdata")
////    }
//  }
//}