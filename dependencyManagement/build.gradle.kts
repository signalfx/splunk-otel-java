plugins {
  `java-platform`
}

val otelVersion = "1.44.1"
val otelAlphaVersion = otelVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelInstrumentationVersion = "2.11.0-SNAPSHOT"
val otelInstrumentationAlphaVersion =  otelInstrumentationVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelContribAlphaVersion = "1.41.0-alpha"

val autoValueVersion = "1.11.0"
val dockerJavaVersion = "3.4.0"
val mockitoVersion = "5.14.2"
val protobufVersion = "4.29.0"
val slf4jVersion = "2.0.16"

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
  api(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.18.2"))
  api(enforcedPlatform("com.google.protobuf:protobuf-bom:$protobufVersion"))
  api(enforcedPlatform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
  api(enforcedPlatform("io.grpc:grpc-bom:1.68.2"))
  api(enforcedPlatform("io.opentelemetry:opentelemetry-bom-alpha:$otelAlphaVersion"))
  api(enforcedPlatform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelInstrumentationAlphaVersion"))
  api(enforcedPlatform("org.junit:junit-bom:5.11.3"))
  api(enforcedPlatform("org.testcontainers:testcontainers-bom:1.20.4"))

  constraints {
    api("com.google.auto.service:auto-service:1.1.1")
    api("org.assertj:assertj-core:3.26.3")
    api("org.awaitility:awaitility:4.2.2")

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
    api("io.opentelemetry.proto:opentelemetry-proto:1.4.0-alpha")
  }
}
