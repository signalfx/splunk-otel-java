plugins {
  id("java-library")
}

dependencies {
  compileOnly(project(":bootstrap"))

  api("io.opentelemetry.javaagent:opentelemetry-testing-common")
  api("com.squareup.okhttp3:okhttp")
  api("org.mockito:mockito-core")
}
