plugins {
  id("groovy")
  id("splunk.instrumentation-conventions")
}

repositories {
  maven {
    url = uri("https://jitpack.io")
  }
}

val otelInstrumentationAlphaVersion: String by extra

dependencies {
  compileOnly("com.github.jkcclemens:khttp:0.1.0")

  testImplementation("com.github.jkcclemens:khttp:0.1.0")

  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
  testRuntimeOnly("io.opentelemetry.javaagent:opentelemetry-armeria-shaded-for-testing:${otelInstrumentationAlphaVersion}:all")
}