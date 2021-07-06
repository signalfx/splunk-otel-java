import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("com.diffplug.spotless")
}

extensions.configure<SpotlessExtension>("spotless") {
  java {
    googleJavaFormat()
    licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public)")
    target("src/**/*.java")
  }
  kotlin {
    // ktfmt() // only supports 4 space indentation
    ktlint().userData(mapOf("indent_size" to "2", "continuation_indent_size" to "2"))
    licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public)")
  }
  format("misc") {
    // not using "**/..." to help keep spotless fast
    target(".gitignore", "*.md", "src/**/.md", "*.sh")
    indentWithSpaces()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

val formatCode by tasks.registering {
  dependsOn(tasks.named("spotlessApply"))
}

tasks.named("check").configure {
  dependsOn(tasks.named("spotlessCheck"))
}
