import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
  id("com.diffplug.spotless")
}

extensions.configure<SpotlessExtension>("spotless") {
  java {
    googleJavaFormat()
    licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public|// Includes work from:)")
    target("src/**/*.java")
    toggleOffOn()
  }
  kotlin {
    // ktfmt() // only supports 4 space indentation
    ktlint().editorConfigOverride(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2",
        "max_line_length" to "160",
        "insert_final_newline" to "true",
        "ktlint_standard_no-wildcard-imports" to "disabled",
        // ktlint does not break up long lines, it just fails on them
        "ktlint_standard_max-line-length" to "disabled",
        // ktlint makes it *very* hard to locate where this actually happened
        "ktlint_standard_trailing-comma-on-call-site" to "disabled",
        // depends on ktlint_standard_wrapping
        "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        // also very hard to find out where this happens
        "ktlint_standard_wrapping" to "disabled"
      )
    )
    licenseHeaderFile(rootProject.file("gradle/spotless.license.java"), "(package|import|public|// Includes work from:)")
  }
  kotlinGradle {
    ktlint().editorConfigOverride(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2",
        "max_line_length" to "160",
        "insert_final_newline" to "true",
        "ktlint_standard_no-wildcard-imports" to "disabled",
        // ktlint does not break up long lines, it just fails on them
        "ktlint_standard_max-line-length" to "disabled",
        // ktlint makes it *very* hard to locate where this actually happened
        "ktlint_standard_trailing-comma-on-call-site" to "disabled",
        // depends on ktlint_standard_wrapping
        "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        // also very hard to find out where this happens
        "ktlint_standard_wrapping" to "disabled"
      )
    )
  }
  format("misc") {
    // not using "**/..." to help keep spotless fast
    target(
      ".gitignore",
      ".gitattributes",
      ".gitconfig",
      ".editorconfig",
      "*.md",
      "src/**/*.md",
      "docs/**/*.md",
      "*.sh",
      "src/**/*.properties",
    )
    leadingTabsToSpaces()
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
