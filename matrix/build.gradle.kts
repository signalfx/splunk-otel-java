import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
  base
  id("com.bmuschko.docker-remote-api") version "6.6.1"
}

fun dockerFileName(template: String) = template.replace("-dockerfile.template", ".dockerfile")

val dockerWorkingDir = project.buildDir.resolve("docker")

val buildTestImagesTask = tasks.create("buildTestImages") {
  group = "build"
  description = "Builds all Docker images for test matrix"
}

val targets = mapOf(
    "jetty" to mapOf(
        "9.4" to listOf("8", "11", "15"),
        "10.0.0.beta3" to listOf("11", "15"),
        "11.0.0.beta3" to listOf("11", "15")
    ),
    "tomcat" to mapOf(
        "7" to listOf("8"),
        "8" to listOf("8", "11"),
        "9" to listOf("8", "11"),
        "10" to listOf("8", "11")
    ),
    "payara" to mapOf(
        "5.2020.6" to listOf("8"),
        "5.2020.6-jdk11" to listOf("11")
    ),
    "wildfly" to mapOf(
        "13.0.0.Final" to listOf("8", "11", "15"),
        "17.0.1.Final" to listOf("8", "11", "15"),
        "21.0.0.Final" to listOf("8", "11", "15")
    ),
    "liberty" to mapOf(
        "20.0.0.12" to listOf("8", "11", "15", "8-jdk-openj9", "11-jdk-openj9", "15-jdk-openj9")
    )
)

targets.forEach { (server, data) ->
  data.forEach { (version, jdks) ->
    jdks.forEach { jdk ->
      val template = "$server-dockerfile.template"

      val prepareTask = tasks.register("${server}ImagePrepare-$version-jdk$jdk", Copy::class) {
        into(dockerWorkingDir)
        from("src") {
          filter(ReplaceTokens::class, "tokens" to mapOf("version" to version, "jdk" to jdk))
          rename { f -> dockerFileName(f) }
        }
        from("app.war")
      }

      val buildTask = tasks.register("${server}Image-$version-jdk$jdk", DockerBuildImage::class) {
        dependsOn(prepareTask)
        group = "build"
        description = "Builds Docker image with $server $version on JDK $jdk"

        inputDir.set(dockerWorkingDir)
        images.add("splunk-$server:$version-jdk$jdk")
        dockerFile.set(dockerWorkingDir.resolve(dockerFileName(template)))
      }

      buildTestImagesTask.dependsOn(buildTask)
    }
  }
}