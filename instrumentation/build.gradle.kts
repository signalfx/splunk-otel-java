plugins {
  id("splunk.shadow-conventions")
}

val instrumentationTest = tasks.named("test")
val instrumentationDeps = dependencies

subprojects {
  val subProj = this

  plugins.withId("java") {
    // Make it so all instrumentation subproject tests can be run with a single command.
    instrumentationTest.configure {
      dependsOn(subProj.tasks.named("test"))
    }

    // exclude stub projects, they are only compile time dependencies
    if (!subProj.name.endsWith("-stub")) {
      instrumentationDeps.run {
        implementation(project(subProj.path))
      }
    }
  }

  tasks {
    compileJava {
      options.release.set(8)
    }
  }
}

tasks {
  shadowJar {
    duplicatesStrategy = DuplicatesStrategy.FAIL
  }
}