plugins {
  `java-platform`
}

val otelVersion = "1.58.0"
val otelAlphaVersion = otelVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelInstrumentationVersion = "2.24.0"
val otelInstrumentationAlphaVersion =  otelInstrumentationVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelContribAlphaVersion = "1.52.0-alpha"

val autoValueVersion = "1.11.1"
val dockerJavaVersion = "3.7.0"
val mockitoVersion = "5.21.0"
val protobufVersion = "4.33.5"
val slf4jVersion = "2.0.17"

// instrumentation version is used to compute Implementation-Version manifest attribute
rootProject.extra["otelInstrumentationVersion"] = otelInstrumentationVersion
rootProject.extra["otelVersion"] = otelVersion
rootProject.extra["otelContribVersion"] = otelContribAlphaVersion.replace("-alpha", "")
rootProject.extra["protobufVersion"] = protobufVersion

javaPlatform {
  // What a great hack!
  allowDependencies()
}

dependencies {

  // BOMs
  api(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.21.0"))
  api(enforcedPlatform("com.google.protobuf:protobuf-bom:$protobufVersion"))
  api(enforcedPlatform("com.squareup.okhttp3:okhttp-bom:5.3.2"))
  api(enforcedPlatform("io.grpc:grpc-bom:1.79.0"))
  api(enforcedPlatform("io.opentelemetry:opentelemetry-bom-alpha:$otelAlphaVersion"))
  api(enforcedPlatform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelInstrumentationAlphaVersion"))
  api(enforcedPlatform("org.junit:junit-bom:6.0.3"))
  api(enforcedPlatform("org.testcontainers:testcontainers-bom:2.0.3"))

  constraints {
    api("com.google.auto.service:auto-service:1.1.1")
    api("org.assertj:assertj-core:3.27.7")
    api("org.awaitility:awaitility:4.3.0")

    api("com.github.docker-java:docker-java-core:$dockerJavaVersion")
    api("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaVersion")

    api("org.mockito:mockito-core:$mockitoVersion")
    api("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("org.slf4j:slf4j-simple:$slf4jVersion")
    api("org.slf4j:jcl-over-slf4j:$slf4jVersion")
    api("com.google.auto.value:auto-value:$autoValueVersion")
    api("com.google.auto.value:auto-value-annotations:$autoValueVersion")

    api("io.opentelemetry.contrib:opentelemetry-samplers:$otelContribAlphaVersion")
    api("io.opentelemetry.contrib:opentelemetry-resource-providers:$otelContribAlphaVersion")
    api("io.opentelemetry.contrib:opentelemetry-opamp-client:${otelContribAlphaVersion}")
    api("io.opentelemetry.proto:opentelemetry-proto:1.9.0-alpha")

    api("org.apache.commons:commons-jexl3:3.6.2")
    api("org.snakeyaml:snakeyaml-engine:2.10")
  }
}
