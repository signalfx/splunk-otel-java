pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "10.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report") version "3.1.4"
    id("com.gradleup.shadow") version "9.6.1"
  }
}

plugins {
  id("com.gradle.develocity") version "4.6.0"
}

val artifactoryUrl: String? = System.getenv("MAVEN_REPOSITORY_URL")
if (artifactoryUrl != null) {
  pluginManagement {
    repositories {
      maven {
        name = "artifactory"
        url = uri(artifactoryUrl)
        credentials(PasswordCredentials::class)
      }
    }
  }
}
dependencyResolutionManagement {
  repositories {
    if (artifactoryUrl != null) {
      maven {
        url = uri(artifactoryUrl)
        credentials(PasswordCredentials::class)
      }
    } else {
      mavenCentral()
    }
    maven {
      name = "sonatypeSnapshots"
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    ivy {
      // Required to source artifact directly from github release page
      // https://github.com/signalfx/csa-releases/releases/download/<version>/oss-agent-mtagent-extension-deployment.jar
      url = uri("https://github.com/")
      metadataSources {
        artifact()
      }
      patternLayout {
        ivy("[organisation]/[module]/releases/download/[revision]/[artifact].[ext]")
        artifact("[organisation]/[module]/releases/download/[revision]/[artifact].[ext]")
      }
    }
  }
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = if (System.getenv("CI") != null) "yes" else "no"
    uploadInBackground = System.getenv("CI") == null

    if (!gradle.startParameter.taskNames.contains(":metadata-generator:generateMetadata")) {
      buildScanPublished {
        File("build-scan.txt").printWriter().use { writer ->
          writer.println(buildScanUri)
        }
      }
    }
  }
}

rootProject.name = "splunk-otel-java"
include(":dependencyManagement")
include(
    "agent",
    "agent-csa-bundle",
    "bootstrap",
    "custom",
    "opamp",
    "instrumentation",
    "instrumentation:compile-stub",
    "instrumentation:glassfish",
    "instrumentation:jdbc",
    "instrumentation:jetty",
    "instrumentation:jvm-metrics",
    "instrumentation:khttp",
    "instrumentation:liberty",
    "instrumentation:nocode",
    "instrumentation:nocode-testing",
    "instrumentation:servlet-3-testing",
    "instrumentation:tomcat",
    "instrumentation:tomee",
    "instrumentation:weblogic",
    "instrumentation:websphere",
    "instrumentation:wildfly",
    "metadata-generator",
    "matrix",
    "profiler",
    "smoke-tests",
    "testing:agent-for-testing",
    "testing:agent-test-extension",
    "testing:jmh-benchmarks",
    "testing:common")
