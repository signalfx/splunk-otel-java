val instrumentationTest = tasks.named("test")
val instrumentationDeps = dependencies

subprojects {
  val subProj = this

  plugins.withId("java") {
    // Make it so all instrumentation subproject tests can be run with a single command.
    instrumentationTest.configure {
      dependsOn(subProj.tasks.named("test"))
    }
  }

  tasks {
    compileJava {
      options.release.set(8)
    }
  }
  dependencies {
    implementation(platform(project(":dependencyManagement")))
  }

}

tasks {
  register("listMuzzleInstrumentations") {
    group = "Help"
    description = "List all instrumentation modules that use muzzle"
    doFirst {
      subprojects
        .filter { it.plugins.hasPlugin("io.opentelemetry.instrumentation.muzzle-check") }
        .map { it.path }
        .forEach { println(it) }
    }
  }
}
