import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

plugins {
  id("war")
  id("com.bmuschko.docker-remote-api")
}

dependencies {
  compileOnly("javax.servlet:javax.servlet-api:4.0.1")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}

val buildProprietaryTestImages by tasks.registering {
  group = "build"
  description = "Builds all Docker images for the test matrix for proprietary app servers"
}

// Intentionally left without group to remain hidden
val pullProprietaryTestImages by tasks.registering

data class Arguments(
  val versions: List<String>,
  val vms: List<String>,
  val jdks: List<String>,
  val extraArgs: Map<String, String> = emptyMap(),
)

data class AppServerTarget(val name: String, val args: List<Arguments>)

val proprietaryTargets = listOf(
  AppServerTarget(
    "weblogic",
    listOf(
      Arguments(versions = listOf("12.1.3", "12.2.1.4"), vms = listOf("hotspot"), jdks = listOf("8"), extraArgs = mapOf("tagSuffix" to "developer")),
      Arguments(versions = listOf("14.1.1.0"), vms = listOf("hotspot"), jdks = listOf("8"), extraArgs = mapOf("tagSuffix" to "developer-8")),
      Arguments(versions = listOf("14.1.1.0"), vms = listOf("hotspot"), jdks = listOf("11"), extraArgs = mapOf("tagSuffix" to "developer-11")),
    ),
  ),
  AppServerTarget(
    "jboss-eap",
    listOf(
      Arguments(versions = listOf("7.1.0"), vms = listOf("hotspot", "openj9"), jdks = listOf("8")),
      Arguments(versions = listOf("7.3.0"), vms = listOf("hotspot", "openj9"), jdks = listOf("8", "11")),
    ),
  ),
)

fun createDockerTasks(targets: List<AppServerTarget>) {
  targets.forEach { (name, args) ->
    args.forEach { (versions, vms, jdks, extraArgs) ->
      versions.forEach { version ->
        vms.forEach { vm ->
          jdks.forEach { jdk ->
            configureImage(name, version, vm, jdk, extraArgs)
          }
        }
      }
    }
  }
}

fun configureImage(server: String, version: String, vm: String, jdk: String, extraArgs: Map<String, String>) {
  val dockerWorkingDir = File(project.buildDir, "docker")
  val dockerFileName = "$server.dockerfile"

  val prepareTask = tasks.register("${server}ImagePrepare-$version-jdk$jdk-$vm", Copy::class) {
    val warTask = project.tasks.named("war", War::class).get()
    dependsOn(warTask)
    into(dockerWorkingDir)
    from("src/$dockerFileName")
    from("src/main/docker/$server")
    from(warTask.archiveFile) {
      rename { "app.war" }
    }
  }

  val vmSuffix = if (vm == "hotspot") "" else "-$vm"
  val fullDockerImageName = "ghcr.io/signalfx/splunk-otel-$server:$version-jdk$jdk$vmSuffix"

  val buildTask = tasks.register("${server}Image-$version-jdk$jdk-$vm", DockerBuildImage::class) {
    group = "build"
    description = "Builds Docker image with $server $version on JDK $jdk-$vm"

    dependsOn(prepareTask)
    inputDir.set(dockerWorkingDir)
    images.add(fullDockerImageName)
    dockerFile.set(File(dockerWorkingDir, dockerFileName))
    buildArgs.set(extraArgs + mapOf("jdk" to jdk, "vm" to vm, "version" to version))
  }

  buildProprietaryTestImages {
    dependsOn(buildTask)
  }

  val pullTask = tasks.register("${server}ImagePull-$version-jdk$jdk-$vm", DockerPullImage::class) {
    image.set(fullDockerImageName)
  }

  pullProprietaryTestImages {
    dependsOn(pullTask)
  }
}

createDockerTasks(proprietaryTargets)
