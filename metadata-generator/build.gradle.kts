dependencies {
  implementation("org.yaml:snakeyaml:2.6")
}

tasks {
  compileJava {
    options.release.set(11)
  }

  val splunkAgentVersion = project.version
  val otelInstrumentationVersion: String = rootProject.extra["otelInstrumentationVersion"] as String
  val otelVersion: String = rootProject.extra["otelVersion"] as String
  val otelContribVersion: String = rootProject.extra["otelContribVersion"] as String
  val outputPath = layout.buildDirectory.file("splunk-otel-java-metadata.yaml")

  register<JavaExec>("generateMetadata") {
    dependsOn(classes)

    outputs.file(outputPath)

    mainClass.set("com.splunk.opentelemetry.tools.MetadataGenerator")

    classpath(sourceSets["main"].runtimeClasspath)

    systemProperty("splunkAgentVersion", splunkAgentVersion)
    systemProperty("otelInstrumentationVersion", otelInstrumentationVersion)
    systemProperty("otelVersion", otelVersion)
    systemProperty("otelContribVersion", otelContribVersion)
    systemProperty("outputPath", outputPath.get().asFile.path)
  }
}
