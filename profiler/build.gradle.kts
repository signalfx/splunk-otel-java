dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  implementation("io.opentelemetry:opentelemetry-proto")
  compileOnly("org.slf4j:slf4j-api")
  compileOnly("io.grpc:grpc-netty")
  compileOnly("io.grpc:grpc-netty-shaded")
  implementation("io.grpc:grpc-protobuf")
  compileOnly("io.grpc:grpc-stub")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("org.slf4j:slf4j-api")
  testImplementation("io.grpc:grpc-netty")
  testImplementation("io.grpc:grpc-stub")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling") {
    exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-tooling-java9")
  }
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-proto")
  testImplementation("io.opentelemetry:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-context")
  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.github.netmikey.logunit:logunit-logback:1.1.0")
}

tasks {
  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}
