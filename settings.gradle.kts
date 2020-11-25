plugins {
  id("com.gradle.enterprise") version "3.4.1"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}
rootProject.name = "splunk-otel-java"
include("agent",
  "instrumentation",
  "instrumentation:middleware",
  "instrumentation:jetty",
  "custom",
  "shared",
  "smoke-tests",
  "matrix")