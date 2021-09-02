dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry:opentelemetry-proto")
  compileOnly("org.slf4j:slf4j-api")
  compileOnly("io.grpc:grpc-netty")
  compileOnly("io.grpc:grpc-netty-shaded")
  compileOnly("io.grpc:grpc-api")
  compileOnly("io.grpc:grpc-protobuf")
  compileOnly("io.grpc:grpc-stub")
  compileOnly("com.google.protobuf:protobuf-java")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("org.slf4j:slf4j-api")
  testImplementation("io.grpc:grpc-netty")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-proto")
  testImplementation("io.opentelemetry:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-context")
  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-proto")
  testImplementation("io.opentelemetry:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
}

tasks {
  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}
