dependencies {
  implementation("org.yaml:snakeyaml:2.5")
}

tasks {
  compileJava {
    options.release.set(11)
  }

  val splunkAgentVersion = project.version
  val otelInstrumentationVersion: String by rootProject.extra
  val otelVersion: String by rootProject.extra
  val otelContribVersion: String by rootProject.extra
  val outputPath = layout.buildDirectory.file("splunk-otel-java-metadata.yaml")

  val generateMetadata by registering(JavaExec::class) {
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
