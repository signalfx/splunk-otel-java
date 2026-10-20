pluginManagement {
  val artifactoryUrl = System.getenv("MAVEN_REPOSITORY_URL")
  repositories {
    if (artifactoryUrl != null) {
      maven {
        name = "artifactory"
        url = uri(artifactoryUrl)
        credentials(PasswordCredentials::class)
      }
    } else {
      gradlePluginPortal()
    }
  }
}
