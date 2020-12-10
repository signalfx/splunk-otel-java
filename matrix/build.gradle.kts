import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
  base
  war
  id("com.bmuschko.docker-remote-api") version "6.6.1"
}

tasks {
  compileJava {
    options.release.set(8)
  }
}

val versions: Map<String, String> by extra

dependencies {
  implementation("javax.servlet:javax.servlet-api:3.0.1")
  implementation("io.opentelemetry:opentelemetry-extension-annotations:${versions["opentelemetry"]}")
}

fun dockerFileName(template: String) = template.replace("-dockerfile.template", ".dockerfile")

val dockerWorkingDir = project.buildDir.resolve("docker")

val buildProprietaryTestImagesTask = tasks.create("buildProprietaryTestImages") {
  group = "build"
  description = "Builds all Docker images for the test matrix for proprietary app servers"
}

val targets = mapOf(
   "weblogic" to mapOf(
        "12.2.1.4" to listOf("developer"),
        "14.1.1.0" to listOf("developer-8", "developer-11")
   )
)

targets.forEach { (server, data) ->
  data.forEach { (version, jdks) ->
    jdks.forEach { jdk ->
      val template = "$server-dockerfile.template"

      val prepareTask = tasks.register("${server}ImagePrepare-$version-jdk$jdk", Copy::class) {
        val warTask = project.tasks.named<Jar>("war").get()
        dependsOn(warTask)
        into(dockerWorkingDir)
        from("src") {
          filter(ReplaceTokens::class, "tokens" to mapOf("version" to version, "jdk" to jdk))
          rename { f -> dockerFileName(f) }
        }
        from(warTask.archiveFile) {
          rename { _ -> "app.war" }
        }
      }

      val buildTask = tasks.register("${server}Image-$version-jdk$jdk", DockerBuildImage::class) {
        dependsOn(prepareTask)
        group = "build"
        description = "Builds Docker image with $server $version on JDK $jdk"

        inputDir.set(dockerWorkingDir)
        images.add("splunk-$server:$version-jdk$jdk")
        dockerFile.set(dockerWorkingDir.resolve(dockerFileName(template)))
      }

      buildProprietaryTestImagesTask.dependsOn(buildTask)
    }
  }
}