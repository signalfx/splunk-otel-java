# Release Process

## Releasing a new version

[This GitHub Action](.github/workflows/release.yaml) builds, uploads and releases a new version of
Splunk Distribution of OpenTelemetry Java Instrumentation
whenever a new tag of the specific format is pushed to the repository.

If you want to release a new version, do the following:

* Checkout the latest version of the `main` branch.
* Find a specific commit, on which the new version should be based.
* Tag that version with the [annotated tag](https://git-scm.com/book/en/v2/Git-Basics-Tagging).
Tag should start with a single lower case letter `v` followed by the desired version number.
E.g. `v0.9.0`.
Unless you have a specific reason to do otherwise, new to-be-released version should be obtained
by incrementing the minor version of [the last released version](https://github.com/signalfx/splunk-otel-java/releases)
* Push newly created tag by running `git push origin <tagname>`.
* GitHub Action will kick in and do the rest.

## Post-release

* Update the release notes for the latest release.
* Update README.md with all changes (new configuration options, new upstream versions, etc)
that happened in the latest release.
* Update CHANGELOG.md and change the Unreleased section to reflect the latest tag.
